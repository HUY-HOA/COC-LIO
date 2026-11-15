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
  const content = JSON.stringify({ enabled: true, version: 2, catalog: [] });
  const env = makeEnvironment(content);
  const response = await worker.fetch(new Request("https://test/v1/content"), env);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("X-COC-LIO-Content-Version"), "2");
  assert.equal(response.headers.get("X-COC-LIO-Content-Store"), "cloudflare-kv");
  assert.equal(response.headers.get("ETag"), '"' + createHash("sha256").update(content).digest("hex") + '"');
  assert.deepEqual(await response.json(), { enabled: true, version: 2, catalog: [] });
});

test("matching ETag returns 304 without a response body", async () => {
  const content = JSON.stringify({ enabled: true, version: 2, catalog: [] });
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

test("missing content pointer is reported as unavailable", async () => {
  const response = await worker.fetch(new Request("https://test/v1/content"), {
    CONTENT: { async get() { return null; } }
  });
  assert.equal(response.status, 503);
});

test("content hash mismatch is rejected", async () => {
  const content = JSON.stringify({ enabled: true, version: 2, catalog: [] });
  const env = makeEnvironment(content);
  env.values.set("content/current", JSON.stringify({
    version: 2,
    key: "content/v2.json",
    sha256: "0".repeat(64)
  }));
  const response = await worker.fetch(new Request("https://test/v1/content"), env);
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error, "CONTENT_INTEGRITY_CHECK_FAILED");
});

test("invalid version keys are rejected", async () => {
  const env = makeEnvironment(JSON.stringify({ enabled: true, version: 2, catalog: [] }));
  env.values.set("content/current", JSON.stringify({
    version: 2,
    key: "../private.json",
    sha256: "0".repeat(64)
  }));
  const response = await worker.fetch(new Request("https://test/v1/content"), env);
  assert.equal(response.status, 503);
});

test("public asset route remains closed until an R2 bucket is bound", async () => {
  const response = await worker.fetch(new Request("https://test/v1/assets/public/icons/king.png"), {});
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error, "ASSET_STORAGE_NOT_ENABLED");
});
