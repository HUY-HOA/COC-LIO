const baseHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
  "Access-Control-Allow-Headers": "If-None-Match, Content-Type",
  "Access-Control-Max-Age": "86400",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer"
};

function safeError(error) {
  if (error instanceof Error) {
    return {
      name: error.name,
      message: String(error.message || "").slice(0, 240),
      stack: String(error.stack || "").split("\n").slice(0, 5).join("\n")
    };
  }
  return { message: String(error).slice(0, 240) };
}

function jsonResponse(body, status = 200, extraHeaders = {}) {
  const headers = new Headers(baseHeaders);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("Cache-Control", "no-store");
  for (const [key, value] of Object.entries(extraHeaders)) headers.set(key, value);
  return new Response(JSON.stringify(body), { status, headers });
}

async function sha256Hex(value) {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, "0")).join("");
}

function matchesEtag(request, etag) {
  return (request.headers.get("If-None-Match") || "")
    .split(",")
    .map(value => value.trim())
    .some(value => value === etag || value === "*");
}

function isValidCatalogItem(item, seenIds) {
  const id = Number(item && item.dataId);
  if (!Number.isSafeInteger(id) || id < 1 ||
      !String(item && item.name || "").trim() || seenIds.has(id)) {
    return false;
  }
  seenIds.add(id);
  return true;
}

async function loadSourceContent(env, manifest) {
  if (!manifest || !Number.isSafeInteger(manifest.version) || manifest.version < 1 ||
      manifest.baseKey !== "source/v" + manifest.version + "/base.json" ||
      !Array.isArray(manifest.shardKeys) || manifest.shardKeys.length > 100) {
    throw new Error("SOURCE_MANIFEST_INVALID");
  }

  const baseRaw = await env.CONTENT.get(manifest.baseKey, { type: "text" });
  if (typeof baseRaw !== "string" || baseRaw.length === 0) {
    throw new Error("SOURCE_BASE_MISSING");
  }

  let base;
  try {
    base = JSON.parse(baseRaw);
  } catch {
    throw new Error("SOURCE_BASE_JSON_INVALID");
  }

  if (!base || base.version !== manifest.version || typeof base.enabled !== "boolean" ||
      !Array.isArray(base.catalog) || !Array.isArray(base.catalogShards) ||
      !Array.isArray(base.guides) || !Array.isArray(base.links) ||
      base.catalogShards.length !== manifest.shardKeys.length ||
      base.guides.length > 20 || base.links.length > 20) {
    throw new Error("SOURCE_BASE_SCHEMA_INVALID");
  }

  const expectedKeys = base.catalogShards.map(name => {
    if (typeof name !== "string" || !/^part-\d{2}\.json$/.test(name)) {
      throw new Error("SOURCE_SHARD_NAME_INVALID");
    }
    return "source/v" + manifest.version + "/" + name;
  });

  if (expectedKeys.some((key, index) => key !== manifest.shardKeys[index])) {
    throw new Error("SOURCE_SHARD_MANIFEST_MISMATCH");
  }

  const shardRawValues = await Promise.all(expectedKeys.map(key =>
    env.CONTENT.get(key, { type: "text" })
  ));
  const seenIds = new Set();
  const catalog = [];

  for (const item of base.catalog) {
    if (!isValidCatalogItem(item, seenIds)) {
      throw new Error("SOURCE_BASE_CATALOG_ITEM_INVALID_OR_DUPLICATE");
    }
    catalog.push(item);
  }

  for (let index = 0; index < shardRawValues.length; index++) {
    const raw = shardRawValues[index];
    if (typeof raw !== "string" || raw.length === 0) {
      throw new Error("SOURCE_SHARD_MISSING:" + expectedKeys[index]);
    }

    let shard;
    try {
      shard = JSON.parse(raw);
    } catch {
      throw new Error("SOURCE_SHARD_JSON_INVALID:" + expectedKeys[index]);
    }

    if (!shard || !Array.isArray(shard.catalog)) {
      throw new Error("SOURCE_SHARD_SCHEMA_INVALID:" + expectedKeys[index]);
    }

    for (const item of shard.catalog) {
      if (!isValidCatalogItem(item, seenIds)) {
        throw new Error("SOURCE_SHARD_ITEM_INVALID_OR_DUPLICATE:" + expectedKeys[index]);
      }
      catalog.push(item);
    }
  }

  if (catalog.length < 1 || catalog.length > 500) {
    throw new Error("SOURCE_CATALOG_COUNT_INVALID");
  }

  const assembled = { ...base, catalog };
  delete assembled.catalogShards;
  const content = JSON.stringify(assembled);
  if (new TextEncoder().encode(content).byteLength > 8 * 1024 * 1024) {
    throw new Error("SOURCE_CONTENT_TOO_LARGE");
  }

  return content;
}

async function publishSourceManifest(env, manifest) {
  const content = await loadSourceContent(env, manifest);
  const digest = await sha256Hex(content);
  const key = "content/v" + manifest.version + ".json";
  const pointer = {
    version: manifest.version,
    key,
    sha256: digest,
    publishedAt: new Date().toISOString()
  };

  let stored = true;
  try {
    await env.CONTENT.put(key, content);
    await env.CONTENT.put("content/current", JSON.stringify(pointer));
  } catch (error) {
    stored = false;
    console.error("COC_LIO_KV_PUBLISH_FAILED", safeError(error));
  }
  return { content, pointer, stored };
}

async function readStoredContent(env, pointer) {
  if (!pointer || !Number.isSafeInteger(pointer.version) || pointer.version < 1 ||
      pointer.key !== "content/v" + pointer.version + ".json" ||
      typeof pointer.sha256 !== "string" ||
      !/^[a-f0-9]{64}$/.test(pointer.sha256)) {
    return null;
  }

  const content = await env.CONTENT.get(pointer.key, { type: "text" });
  if (typeof content !== "string" || await sha256Hex(content) !== pointer.sha256) return null;

  let decoded;
  try {
    decoded = JSON.parse(content);
  } catch {
    return null;
  }

  if (!decoded || decoded.version !== pointer.version ||
      !Array.isArray(decoded.catalog) || decoded.catalog.length < 1 ||
      decoded.catalog.length > 500) {
    return null;
  }
  return { content, pointer };
}

function contentResponse(request, content, pointer, store = "cloudflare-kv") {
  const etag = '"' + pointer.sha256 + '"';
  const headers = new Headers(baseHeaders);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("Cache-Control", "public, max-age=30, s-maxage=60, stale-while-revalidate=300");
  headers.set("ETag", etag);
  headers.set("X-COC-LIO-Content-Version", String(pointer.version));
  headers.set("X-COC-LIO-Content-Hash", pointer.sha256);
  headers.set("X-COC-LIO-Content-Store", store);

  if (matchesEtag(request, etag)) return new Response(null, { status: 304, headers });
  return new Response(request.method === "HEAD" ? null : content, { status: 200, headers });
}

async function servePublicAsset(request, env, pathname) {
  if (!env.ASSETS) {
    return jsonResponse({ error: "ASSET_STORAGE_NOT_ENABLED" }, 503);
  }

  let relativePath;
  try {
    relativePath = decodeURIComponent(pathname.slice("/v1/assets/public/".length));
  } catch {
    return jsonResponse({ error: "INVALID_ASSET_PATH" }, 400);
  }

  if (!relativePath || relativePath.length > 400 || relativePath.includes("..") ||
      relativePath.includes("\\") || relativePath.includes("\0")) {
    return jsonResponse({ error: "INVALID_ASSET_PATH" }, 400);
  }

  try {
    const object = await env.ASSETS.get("public/" + relativePath);
    if (!object) return jsonResponse({ error: "ASSET_NOT_FOUND" }, 404);

    const headers = new Headers(baseHeaders);
    object.writeHttpMetadata(headers);
    headers.set("ETag", object.httpEtag);
    headers.set("Cache-Control", "public, max-age=31536000, immutable");
    headers.set("Content-Length", String(object.size));

    return new Response(request.method === "HEAD" ? null : object.body, {
      status: 200,
      headers
    });
  } catch (error) {
    console.error("COC_LIO_R2_ASSET_READ_FAILED", safeError(error));
    return jsonResponse({ error: "ASSET_READ_FAILED" }, 502);
  }
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: baseHeaders });
    }
    if (request.method !== "GET" && request.method !== "HEAD") {
      return jsonResponse({ error: "METHOD_NOT_ALLOWED" }, 405, { Allow: "GET, HEAD, OPTIONS" });
    }
    if (url.pathname === "/health") {
      return jsonResponse({
        ok: true,
        service: "coc-lio-remote-content",
        storage: "cloudflare-kv"
      });
    }
    if (url.pathname.startsWith("/v1/assets/public/")) {
      return servePublicAsset(request, env, url.pathname);
    }
    if (url.pathname !== "/v1/content") return jsonResponse({ error: "NOT_FOUND" }, 404);

    try {
      let pointer = await env.CONTENT.get("content/current", { type: "json" });
      let stored = await readStoredContent(env, pointer);
      let manifest = await env.CONTENT.get("source/current", { type: "json" });

      if (manifest && (!stored || manifest.version > (pointer?.version || 0))) {
        try {
          const latest = await publishSourceManifest(env, manifest);
          if (latest) {
            return contentResponse(
              request,
              latest.content,
              latest.pointer,
              latest.stored ? "cloudflare-kv" : "cloudflare-kv-source"
            );
          }
        } catch (error) {
          console.error("COC_LIO_SOURCE_SYNC_FAILED", safeError(error));
        }
      }

      pointer = await env.CONTENT.get("content/current", { type: "json" });
      stored = await readStoredContent(env, pointer);
      if (!stored) return jsonResponse({ error: "CONTENT_SERVICE_UNAVAILABLE" }, 503);
      return contentResponse(request, stored.content, stored.pointer);
    } catch (error) {
      console.error("COC_LIO_WORKER_REQUEST_FAILED", safeError(error));
      return jsonResponse({ error: "CONTENT_SERVICE_UNAVAILABLE" }, 502);
    }
  }
};
