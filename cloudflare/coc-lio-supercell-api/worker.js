const FIREBASE_JWKS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const SUPERCELL_PROXY = "https://cocproxy.royaleapi.dev";
let cachedJwks = null;
let cachedJwksUntil = 0;

function jsonResponse(payload, status = 200, headers = {}) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, OPTIONS",
      "Access-Control-Allow-Headers": "Authorization, Content-Type",
      ...headers
    }
  });
}

function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - normalized.length % 4) % 4);
  const binary = atob(padded);
  return Uint8Array.from(binary, character => character.charCodeAt(0));
}

function decodeJwtPart(value) {
  return JSON.parse(new TextDecoder().decode(decodeBase64Url(value)));
}

async function getFirebaseJwks() {
  const now = Date.now();
  if (cachedJwks && now < cachedJwksUntil) return cachedJwks;
  const response = await fetch(FIREBASE_JWKS_URL, {
    headers: { "Accept": "application/json" }
  });
  if (!response.ok) throw new Error("Không tải được khóa xác thực Firebase.");
  const result = await response.json();
  if (!Array.isArray(result.keys) || result.keys.length === 0) {
    throw new Error("Khóa xác thực Firebase không hợp lệ.");
  }
  const cacheControl = response.headers.get("Cache-Control") || "";
  const maxAge = Number(cacheControl.match(/max-age=(\d+)/i)?.[1] || 3600);
  cachedJwks = result.keys;
  cachedJwksUntil = now + Math.min(Math.max(maxAge, 300), 21600) * 1000;
  return cachedJwks;
}

async function verifyFirebaseToken(request, projectId) {
  const authorization = request.headers.get("Authorization") || "";
  const token = authorization.startsWith("Bearer ") ? authorization.slice(7).trim() : "";
  if (!token || token.length > 8192) return false;

  const segments = token.split(".");
  if (segments.length !== 3) return false;

  try {
    const header = decodeJwtPart(segments[0]);
    const claims = decodeJwtPart(segments[1]);
    if (header.alg !== "RS256" || typeof header.kid !== "string") return false;
    if (claims.aud !== projectId ||
        claims.iss !== `https://securetoken.google.com/${projectId}` ||
        typeof claims.sub !== "string" ||
        claims.sub.length < 1 ||
        claims.sub.length > 128) return false;

    const now = Math.floor(Date.now() / 1000);
    if (!Number.isFinite(claims.exp) || claims.exp <= now ||
        !Number.isFinite(claims.iat) || claims.iat > now + 300 ||
        !Number.isFinite(claims.auth_time) || claims.auth_time > now + 300) return false;

    const keys = await getFirebaseJwks();
    const jwk = keys.find(key => key.kid === header.kid && key.kty === "RSA");
    if (!jwk) return false;

    const publicKey = await crypto.subtle.importKey(
      "jwk",
      jwk,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"]
    );
    return await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      publicKey,
      decodeBase64Url(segments[2]),
      new TextEncoder().encode(`${segments[0]}.${segments[1]}`)
    );
  } catch {
    return false;
  }
}

function resolveSupercellPath(pathname) {
  const parts = pathname.split("/").filter(Boolean);
  if (parts.length !== 3 && parts.length !== 4) return null;
  if (parts[0] !== "v1") return null;

  const collection = parts[1];
  if (collection !== "players" && collection !== "clans") return null;
  let tag;
  try {
    tag = decodeURIComponent(parts[2]).trim().toUpperCase().replace(/^#/, "");
  } catch {
    return null;
  }
  if (!/^[PYLQGRJCUV0289]{3,15}$/.test(tag)) return null;

  let suffix = "";
  if (parts.length === 4) {
    if (collection !== "clans" || !["members", "warlog"].includes(parts[3])) return null;
    suffix = `/${parts[3]}`;
  }

  return `/v1/${collection}/${encodeURIComponent("#" + tag)}${suffix}`;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, OPTIONS",
          "Access-Control-Allow-Headers": "Authorization, Content-Type",
          "Access-Control-Max-Age": "86400"
        }
      });
    }

    if (url.pathname === "/health" && request.method === "GET") {
      return jsonResponse({
        service: "COC-LIO Supercell API",
        status: "ready",
        supercellKeyConfigured: Boolean(env.SUPERCELL_API_TOKEN),
        authentication: "Firebase ID token",
        upstream: "RoyaleAPI Clash of Clans proxy",
        readOnly: true
      });
    }

    if (request.method !== "GET") {
      return jsonResponse({ error: "Chỉ hỗ trợ yêu cầu GET." }, 405, { Allow: "GET, OPTIONS" });
    }

    const upstreamPath = resolveSupercellPath(url.pathname);
    if (!upstreamPath || url.search) {
      return jsonResponse({ error: "Endpoint không được hỗ trợ." }, 404);
    }

    if (!env.FIREBASE_PROJECT_ID) {
      return jsonResponse({ error: "Thiếu cấu hình xác thực Firebase." }, 503);
    }
    if (!await verifyFirebaseToken(request, env.FIREBASE_PROJECT_ID)) {
      return jsonResponse({ error: "Cần đăng nhập COC-LIO để sử dụng dữ liệu Supercell." }, 401);
    }
    if (!env.SUPERCELL_API_TOKEN) {
      return jsonResponse({ error: "Máy chủ chưa được cấu hình API Key Supercell." }, 503);
    }

    const cacheKey = new Request(`https://coc-lio-supercell-cache.invalid${upstreamPath}`, { method: "GET" });
    const cache = caches.default;
    try {
      const cached = await cache.match(cacheKey);
      if (cached) {
        const headers = new Headers(cached.headers);
        headers.set("Access-Control-Allow-Origin", "*");
        return new Response(cached.body, { status: cached.status, headers });
      }
    } catch {
    }

    try {
      const upstream = await fetch(`${SUPERCELL_PROXY}${upstreamPath}`, {
        method: "GET",
        headers: {
          "Accept": "application/json",
          "Authorization": `Bearer ${env.SUPERCELL_API_TOKEN}`
        }
      });
      const body = await upstream.text();
      const headers = new Headers({
        "Content-Type": upstream.headers.get("Content-Type") || "application/json; charset=utf-8",
        "Cache-Control": upstream.ok ? "public, max-age=60" : "no-store",
        "Access-Control-Allow-Origin": "*"
      });
      const response = new Response(body, { status: upstream.status, headers });
      if (upstream.ok) {
        try {
          await cache.put(cacheKey, response.clone());
        } catch {
        }
      }
      return response;
    } catch {
      return jsonResponse({ error: "Không kết nối được máy chủ Supercell. Vui lòng thử lại." }, 502);
    }
  }
};
