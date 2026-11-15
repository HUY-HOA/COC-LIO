const baseHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
  "Access-Control-Allow-Headers": "If-None-Match, Content-Type",
  "Access-Control-Max-Age": "86400",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer"
};

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

    if (url.pathname !== "/v1/content") {
      return jsonResponse({ error: "NOT_FOUND" }, 404);
    }

    try {
      const pointer = await env.CONTENT.get("content/current", { type: "json", cacheTtl: 60 });
      if (!pointer || !Number.isInteger(pointer.version) || pointer.version < 1 ||
          typeof pointer.key !== "string" ||
          !/^content\/v[1-9][0-9]*\.json$/.test(pointer.key) ||
          typeof pointer.sha256 !== "string" ||
          !/^[a-f0-9]{64}$/.test(pointer.sha256)) {
        return jsonResponse({ error: "CONTENT_NOT_PUBLISHED" }, 503);
      }

      const content = await env.CONTENT.get(pointer.key, { type: "text", cacheTtl: 60 });
      if (typeof content !== "string") {
        return jsonResponse({ error: "CONTENT_VERSION_MISSING" }, 503);
      }

      const digest = await sha256Hex(content);
      if (digest !== pointer.sha256) {
        return jsonResponse({ error: "CONTENT_INTEGRITY_CHECK_FAILED" }, 503);
      }

      let decoded;
      try {
        decoded = JSON.parse(content);
      } catch {
        return jsonResponse({ error: "CONTENT_JSON_INVALID" }, 503);
      }
      if (!decoded || decoded.version !== pointer.version || !Array.isArray(decoded.catalog) ||
          decoded.catalog.length > 500) {
        return jsonResponse({ error: "CONTENT_SCHEMA_INVALID" }, 503);
      }

      const etag = '"' + digest + '"';
      const headers = new Headers(baseHeaders);
      headers.set("Content-Type", "application/json; charset=utf-8");
      headers.set("Cache-Control", "public, max-age=30, s-maxage=60, stale-while-revalidate=300");
      headers.set("ETag", etag);
      headers.set("X-COC-LIO-Content-Version", String(pointer.version));
      headers.set("X-COC-LIO-Content-Hash", digest);
      headers.set("X-COC-LIO-Content-Store", "cloudflare-kv");

      if (matchesEtag(request, etag)) {
        return new Response(null, { status: 304, headers });
      }

      return new Response(request.method === "HEAD" ? null : content, { status: 200, headers });
    } catch {
      return jsonResponse({ error: "CONTENT_SERVICE_UNAVAILABLE" }, 502);
    }
  }
};
