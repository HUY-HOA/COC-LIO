const baseHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
  "Access-Control-Allow-Headers": "If-None-Match, Content-Type",
  "Access-Control-Max-Age": "86400",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer"
};

const GITHUB_RAW_ROOT =
  "https://raw.githubusercontent.com/HUY-HOA-1/COC-LIO/main/";

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

async function fetchRepositoryJson(path) {
  const response = await fetch(GITHUB_RAW_ROOT + path, {
    headers: { Accept: "application/json" },
    cf: { cacheTtl: 60, cacheEverything: true }
  });
  if (!response.ok) throw new Error("GitHub content fetch failed: " + path);
  return response.json();
}

async function publishLatestRepositoryContent(env, existingPointer) {
  const base = await fetchRepositoryJson("remote-content/current.json");
  if (!base || !Number.isInteger(base.version) || base.version < 1 ||
      typeof base.enabled !== "boolean" || !Array.isArray(base.catalog) ||
      !Array.isArray(base.guides) || !Array.isArray(base.links)) {
    throw new Error("Repository content base schema is invalid");
  }

  if (existingPointer && base.version <= existingPointer.version) return null;

  const shardNames = base.catalogShards || [];
  if (!Array.isArray(shardNames) || shardNames.length > 100 ||
      shardNames.some(name => typeof name !== "string" || !/^part-\d{2}\.json$/.test(name))) {
    throw new Error("Repository catalog shard manifest is invalid");
  }

  const seenIds = new Set();
  const catalog = [];
  for (const item of base.catalog) {
    const id = Number(item && item.dataId);
    if (!Number.isInteger(id) || id < 1 || seenIds.has(id) ||
        !String(item && item.name || "").trim()) {
      throw new Error("Repository base catalog contains an invalid or duplicate dataId");
    }
    seenIds.add(id);
    catalog.push(item);
  }

  const shards = await Promise.all(shardNames.map(name =>
    fetchRepositoryJson("remote-content/catalog/" + name)
  ));

  for (let index = 0; index < shards.length; index++) {
    const shard = shards[index];
    if (!shard || !Array.isArray(shard.catalog)) {
      throw new Error("Catalog shard has no catalog array: " + shardNames[index]);
    }
    for (const item of shard.catalog) {
      const id = Number(item && item.dataId);
      if (!Number.isInteger(id) || id < 1 || seenIds.has(id) ||
          !String(item && item.name || "").trim()) {
        throw new Error("Invalid or duplicate dataId in " + shardNames[index]);
      }
      seenIds.add(id);
      catalog.push(item);
    }
  }

  if (catalog.length > 500) throw new Error("Catalog exceeds the app limit");
  if (base.guides.length > 20 || base.links.length > 20) {
    throw new Error("Remote content exceeds the supported item limits");
  }

  const assembled = { ...base, catalog };
  delete assembled.catalogShards;
  const content = JSON.stringify(assembled);
  if (new TextEncoder().encode(content).byteLength > 8 * 1024 * 1024) {
    throw new Error("Remote content exceeds the safe 8 MiB response limit");
  }

  const digest = await sha256Hex(content);
  const key = "content/v" + base.version + ".json";
  const pointer = {
    version: base.version,
    key,
    sha256: digest,
    publishedAt: new Date().toISOString()
  };

  await env.CONTENT.put(key, content);
  await env.CONTENT.put("content/current", JSON.stringify(pointer));
  return { content, pointer };
}

async function readStoredContent(env, pointer) {
  if (!pointer || !Number.isInteger(pointer.version) || pointer.version < 1 ||
      typeof pointer.key !== "string" ||
      !/^content\/v[1-9][0-9]*\.json$/.test(pointer.key) ||
      typeof pointer.sha256 !== "string" ||
      !/^[a-f0-9]{64}$/.test(pointer.sha256)) {
    return null;
  }

  const content = await env.CONTENT.get(pointer.key, { type: "text", cacheTtl: 60 });
  if (typeof content !== "string") return null;
  if (await sha256Hex(content) !== pointer.sha256) return null;

  let decoded;
  try {
    decoded = JSON.parse(content);
  } catch {
    return null;
  }

  if (!decoded || decoded.version !== pointer.version ||
      !Array.isArray(decoded.catalog) || decoded.catalog.length > 500) {
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
  } catch {
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
      return jsonResponse({ ok: true, service: "coc-lio-remote-content", storage: "cloudflare-kv" });
    }
    if (url.pathname.startsWith("/v1/assets/public/")) {
      return servePublicAsset(request, env, url.pathname);
    }
    if (url.pathname !== "/v1/content") return jsonResponse({ error: "NOT_FOUND" }, 404);

    try {
      let pointer = await env.CONTENT.get("content/current", { type: "json", cacheTtl: 60 });
      try {
        const latest = await publishLatestRepositoryContent(env, pointer);
        if (latest) return contentResponse(request, latest.content, latest.pointer);
      } catch {
      }

      pointer = await env.CONTENT.get("content/current", { type: "json", cacheTtl: 60 });
      const stored = await readStoredContent(env, pointer);
      if (!stored) return jsonResponse({ error: "CONTENT_SERVICE_UNAVAILABLE" }, 503);
      return contentResponse(request, stored.content, stored.pointer);
    } catch {
      return jsonResponse({ error: "CONTENT_SERVICE_UNAVAILABLE" }, 502);
    }
  }
};
