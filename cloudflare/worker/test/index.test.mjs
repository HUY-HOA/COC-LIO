import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { test } from "node:test";
import worker from "../src/index.mjs";

function makeEnvironment(content, overrides = {}) {
  const digest = createHash("sha256").update(content).digest("hex");
  const pointer = {
    version: 2,
    key: "content/v2.json",
    sha256: digest
  };
  const values = new Map([
    ["content/current", JSON.stringify(pointer)],
    ["content/v2.json", content]
  ]);
  return {
    CONTENT: {
      async get(key, options) {
        const value = values.get(key);
        if (value == null) return null;
        return options?.type === "json" ? JSON.parse(value) : value;
      },
      async put(key, value) {
        values.set(key, value);
      }
    },
    values,
    ...overrides
  };
}

test("health endpoint is available without reading the content store", async () => {
  const response = await worker.fetch(new Request("https://test/health"), {});
  assert.equal(response.status, 200);
  assert.equal((await response.json()).ok, true);
});

test("content endpoint returns verified JSON and version headers", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response("", { status: 503 });
  try {
    const content = JSON.stringify({ enabled: true, version: 2, catalog: [] });
    const env = makeEnvironment(content);
    const response = await worker.fetch(new Request("https://test/v1/content"), env);
    assert.equal(response.status, 200);
    assert.equal(response.headers.get("X-COC-LIO-Content-Version"), "2");
    assert.equal(response.headers.get("X-COC-LIO-Content-Store"), "cloudflare-kv");
    assert.equal(response.headers.get("ETag"), '"' + createHash("sha256").update(content).digest("hex") + '"');
    assert.deepEqual(await response.json(), { enabled: true, version: 2, catalog: [] });
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("matching ETag returns 304 without a response body", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response("", { status: 503 });
  try {
    const content = JSON.stringify({ enabled: true, version: 2, catalog: [] });
    const env = makeEnvironment(content);
    const first = await worker.fetch(new Request("https://test/v1/content"), env);
    const etag = first.headers.get("ETag");
    const second = await worker.fetch(new Request("https://test/v1/content", {
      headers: { "If-None-Match": etag }
    }), env);
    assert.equal(second.status, 304);
    assert.equal(await second.text(), "");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("write methods are rejected", async () => {
  const response = await worker.fetch(new Request("https://test/v1/content", { method: "POST" }), {});
  assert.equal(response.status, 405);
});

test("missing content pointer is reported as unavailable when GitHub is unavailable", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response("", { status: 503 });
  try {
    const response = await worker.fetch(new Request("https://test/v1/content"), {
      CONTENT: { async get() { return null; }, async put() {} }
    });
    assert.equal(response.status, 503);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("content hash mismatch is rejected", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response("", { status: 503 });
  try {
    const content = JSON.stringify({ enabled: true, version: 2, catalog: [] });
    const env = makeEnvironment(content);
    env.values.set("content/current", JSON.stringify({
      version: 2,
      key: "content/v2.json",
      sha256: "0".repeat(64)
    }));
    const response = await worker.fetch(new Request("https://test/v1/content"), env);
    assert.equal(response.status, 503);
    assert.equal((await response.json()).error, "CONTENT_SERVICE_UNAVAILABLE");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("invalid version keys are rejected", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response("", { status: 503 });
  try {
    const env = makeEnvironment(JSON.stringify({ enabled: true, version: 2, catalog: [] }));
    env.values.set("content/current", JSON.stringify({
      version: 2,
      key: "../private.json",
      sha256: "0".repeat(64)
    }));
    const response = await worker.fetch(new Request("https://test/v1/content"), env);
    assert.equal(response.status, 503);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("Worker imports, validates, merges and stores JSON catalog shards on a new version", async () => {
  const originalFetch = globalThis.fetch;
  const base = {
    enabled: true,
    version: 3,
    catalog: [],
    catalogShards: ["part-01.json"],
    guides: [],
    links: []
  };
  const shard = { catalog: [{ dataId: 4000000, name: "Barbarian" }] };
  globalThis.fetch = async url => {
    if (String(url).endsWith("/remote-content/current.json")) {
      return new Response(JSON.stringify(base), { status: 200 });
    }
    if (String(url).endsWith("/remote-content/catalog/part-01.json")) {
      return new Response(JSON.stringify(shard), { status: 200 });
    }
    return new Response("", { status: 404 });
  };
  const values = new Map();
  const env = {
    CONTENT: {
      async get(key, options) {
        const value = values.get(key);
        if (value == null) return null;
        return options?.type === "json" ? JSON.parse(value) : value;
      },
      async put(key, value) { values.set(key, value); }
    }
  };
  try {
    const response = await worker.fetch(new Request("https://test/v1/content"), env);
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.equal(body.version, 3);
    assert.equal(body.catalog.length, 1);
    assert.equal(body.catalog[0].dataId, 4000000);
    assert.equal(values.has("content/v3.json"), true);
    assert.equal(values.has("content/current"), true);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("catalog shard duplicates are rejected and existing version remains served", async () => {
  const originalFetch = globalThis.fetch;
  const base = {
    enabled: true,
    version: 3,
    catalog: [{ dataId: 4000000, name: "Barbarian" }],
    catalogShards: ["part-01.json"],
    guides: [],
    links: []
  };
  const shard = { catalog: [{ dataId: 4000000, name: "Duplicate" }] };
  globalThis.fetch = async url => String(url).endsWith("/remote-content/current.json")
    ? new Response(JSON.stringify(base), { status: 200 })
    : new Response(JSON.stringify(shard), { status: 200 });
  const oldContent = JSON.stringify({ enabled: true, version: 2, catalog: [] });
  const env = makeEnvironment(oldContent);
  try {
    const response = await worker.fetch(new Request("https://test/v1/content"), env);
    assert.equal(response.status, 200);
    assert.equal((await response.json()).version, 2);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("public asset route remains closed until an R2 bucket is bound", async () => {
  const response = await worker.fetch(new Request("https://test/v1/assets/public/icons/king.png"), {});
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error, "ASSET_STORAGE_NOT_ENABLED");
});
