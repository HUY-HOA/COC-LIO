import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { test } from "node:test";
import worker from "../src/index.mjs";

function sha256(value) {
  return createHash("sha256").update(value).digest("hex");
}

function makeEnvironment(content, overrides = {}) {
  const value = JSON.stringify(content);
  const pointer = {
    version: content.version,
    key: "content/v" + content.version + ".json",
    sha256: sha256(value)
  };
  const values = new Map([
    ["content/current", JSON.stringify(pointer)],
    [pointer.key, value]
  ]);
  return {
    CONTENT: {
      async get(key, options) {
        const raw = values.get(key);
        if (raw == null) return null;
        return options?.type === "json" ? JSON.parse(raw) : raw;
      },
      async put(key, raw) {
        values.set(key, raw);
      }
    },
    values,
    ...overrides
  };
}

function sourceFixture(version = 3, shardNames = ["part-01.json"]) {
  const base = {
    enabled: true,
    version,
    catalog: [],
    catalogShards: shardNames,
    guides: [],
    links: []
  };
  const shards = Object.fromEntries(shardNames.map((name, index) => [
    "source/v" + version + "/" + name,
    JSON.stringify({ catalog: [{ dataId: 4000000 + index, name: "Catalog " + index }] })
  ]));
  const manifest = {
    version,
    baseKey: "source/v" + version + "/base.json",
    shardKeys: shardNames.map(name => "source/v" + version + "/" + name)
  };
  const values = new Map([
    [manifest.baseKey, JSON.stringify(base)],
    ["source/current", JSON.stringify(manifest)],
    ...Object.entries(shards)
  ]);
  return { manifest, values };
}

function kvEnvironment(values = new Map()) {
  return {
    values,
    CONTENT: {
      async get(key, options) {
        const raw = values.get(key);
        if (raw == null) return null;
        return options?.type === "json" ? JSON.parse(raw) : raw;
      },
      async put(key, value) {
        values.set(key, value);
      }
    }
  };
}

test("health endpoint is available without reading the content store", async () => {
  const response = await worker.fetch(new Request("https://test/health"), {});
  assert.equal(response.status, 200);
  assert.equal((await response.json()).ok, true);
});

test("content endpoint bootstraps the catalog directly from private-source KV shards", async () => {
  const fixture = sourceFixture(3, ["part-01.json", "part-02.json"]);
  const env = kvEnvironment(fixture.values);
  const response = await worker.fetch(new Request("https://test/v1/content"), env);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("X-COC-LIO-Content-Version"), "3");
  assert.equal(response.headers.get("X-COC-LIO-Content-Store"), "cloudflare-kv");
  const body = await response.json();
  assert.equal(body.catalog.length, 2);
  assert.deepEqual(body.catalog.map(item => item.dataId), [4000000, 4000001]);
  assert.equal("catalogShards" in body, false);
  assert.equal(env.values.has("content/v3.json"), true);
  assert.equal(env.values.has("content/current"), true);
});

test("Worker does not request a private GitHub repository at runtime", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => {
    throw new Error("Network access should not be needed");
  };
  try {
    const fixture = sourceFixture();
    const response = await worker.fetch(new Request("https://test/v1/content"), kvEnvironment(fixture.values));
    assert.equal(response.status, 200);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("matching ETag returns 304 without a response body", async () => {
  const content = { enabled: true, version: 2, catalog: [{ dataId: 4, name: "Archer" }] };
  const env = makeEnvironment(content);
  const first = await worker.fetch(new Request("https://test/v1/content"), env);
  const etag = first.headers.get("ETag");
  const second = await worker.fetch(new Request("https://test/v1/content", {
    headers: { "If-None-Match": etag }
  }), env);
  assert.equal(second.status, 304);
  assert.equal(await second.text(), "");
});

test("write methods are rejected", async () => {
  const response = await worker.fetch(new Request("https://test/v1/content", { method: "POST" }), {});
  assert.equal(response.status, 405);
});

test("invalid source manifest is rejected and does not publish content", async () => {
  const env = kvEnvironment(new Map([["source/current", JSON.stringify({
    version: 3,
    baseKey: "../private.json",
    shardKeys: []
  })]]));
  const response = await worker.fetch(new Request("https://test/v1/content"), env);
  assert.equal(response.status, 503);
  assert.equal(env.values.has("content/current"), false);
});

test("missing source shard does not overwrite existing valid content", async () => {
  const oldContent = { enabled: true, version: 2, catalog: [{ dataId: 2, name: "Old" }] };
  const env = makeEnvironment(oldContent);
  const fixture = sourceFixture(3, ["part-01.json", "part-02.json"]);
  for (const [key, value] of fixture.values) env.values.set(key, value);
  env.values.delete("source/v3/part-02.json");
  const response = await worker.fetch(new Request("https://test/v1/content"), env);
  assert.equal(response.status, 200);
  assert.equal((await response.json()).version, 2);
});

test("duplicate data IDs across source shards are rejected", async () => {
  const fixture = sourceFixture(3);
  fixture.values.set(
    "source/v3/part-01.json",
    JSON.stringify({ catalog: [{ dataId: 4, name: "Duplicate" }, { dataId: 4, name: "Duplicate again" }] })
  );
  const oldContent = { enabled: true, version: 2, catalog: [{ dataId: 2, name: "Old" }] };
  const env = makeEnvironment(oldContent);
  for (const [key, value] of fixture.values) env.values.set(key, value);
  const response = await worker.fetch(new Request("https://test/v1/content"), env);
  assert.equal(response.status, 200);
  assert.equal((await response.json()).version, 2);
});

test("content hash mismatch is rejected", async () => {
  const env = makeEnvironment({ enabled: true, version: 2, catalog: [{ dataId: 4, name: "Archer" }] });
  env.values.set("content/current", JSON.stringify({
    version: 2,
    key: "content/v2.json",
    sha256: "0".repeat(64)
  }));
  const response = await worker.fetch(new Request("https://test/v1/content"), env);
  assert.equal(response.status, 503);
});

test("newer source version supersedes valid cached content", async () => {
  const oldContent = { enabled: true, version: 2, catalog: [{ dataId: 2, name: "Old" }] };
  const env = makeEnvironment(oldContent);
  const fixture = sourceFixture(3, ["part-01.json", "part-02.json"]);
  for (const [key, value] of fixture.values) env.values.set(key, value);
  const response = await worker.fetch(new Request("https://test/v1/content"), env);
  assert.equal(response.status, 200);
  assert.equal((await response.json()).version, 3);
});

test("public asset route remains closed until an R2 bucket is bound", async () => {
  const response = await worker.fetch(new Request("https://test/v1/assets/public/icons/king.png"), {});
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error, "ASSET_STORAGE_NOT_ENABLED");
});
