import test from "node:test";
import assert from "node:assert/strict";
import worker from "./index.js";

function makeEnv({ limit = 3, aiRun } = {}) {
  const store = new Map();
  const limiter = { limit: async () => ({ success: true }) };
  return {
    DAILY_FREE_LIMIT: String(limit),
    LIMITS: {
      get: async key => store.get(key) ?? null,
      put: async (key, value) => { store.set(key, value); }
    },
    DEVICE_RATE_LIMITER: limiter,
    IP_RATE_LIMITER: limiter,
    AI: { run: aiRun ?? (async () => ({ image: "aGVsbG8=" })) },
    store
  };
}

function request(path, body, headers = {}) {
  return new Request("https://example.test" + path, {
    method: body === undefined ? "GET" : "POST",
    headers: {
      "X-Device-Id": "device-test-123456",
      ...headers,
      ...(body === undefined ? {} : { "content-type": "application/json" })
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
}

test("rejects requests without a device identifier", async () => {
  const env = makeEnv();
  const response = await worker.fetch(new Request("https://example.test/credits"), env);
  assert.equal(response.status, 400);
});

test("credits starts at the configured daily limit", async () => {
  const response = await worker.fetch(request("/credits"), makeEnv({ limit: 4 }));
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { credits: 4, limit: 4 });
});

test("generation decodes Cloudflare's base64 image response", async () => {
  let calledModel = "";
  const env = makeEnv({
    aiRun: async (model, input) => {
      calledModel = model;
      assert.match(input.prompt, /photorealistic/);
      return { image: "aGVsbG8=" };
    }
  });
  const response = await worker.fetch(request("/generate", { prompt: "A blue bird", style: 0 }), env);
  assert.equal(response.status, 200);
  const result = await response.json();
  assert.equal(result.mimeType, "image/jpeg");
  assert.equal(atob(result.image), "hello");
  assert.equal(calledModel, "@cf/black-forest-labs/flux-1-schnell");
});

test("edit uses the supported FLUX.2 klein multipart input contract", async () => {
  let calledModel = "";
  let multipart = false;
  const env = makeEnv({
    aiRun: async (model, input) => {
      calledModel = model;
      multipart = Boolean(input.multipart?.body && input.multipart?.contentType?.includes("multipart/form-data"));
      return { image: "aGVsbG8=" };
    }
  });
  const response = await worker.fetch(request("/edit", {
    prompt: "Turn this into watercolor",
    image: "aGVsbG8="
  }), env);
  assert.equal(response.status, 200);
  assert.equal(calledModel, "@cf/black-forest-labs/flux-2-klein-4b");
  assert.equal(multipart, true);
});

test("invalid edit images are rejected without consuming a credit", async () => {
  const env = makeEnv({ limit: 2 });
  const response = await worker.fetch(request("/edit", {
    prompt: "Edit this",
    image: "not-base64!"
  }), env);
  assert.equal(response.status, 400);
  const credits = await worker.fetch(request("/credits"), env);
  assert.deepEqual(await credits.json(), { credits: 2, limit: 2 });
});

test("daily credit limit blocks inference", async () => {
  let calls = 0;
  const env = makeEnv({
    limit: 1,
    aiRun: async () => { calls++; return { image: "aGVsbG8=" }; }
  });
  assert.equal((await worker.fetch(request("/generate", { prompt: "first" }), env)).status, 200);
  assert.equal((await worker.fetch(request("/generate", { prompt: "second" }), env)).status, 429);
  assert.equal(calls, 1);
});

test("rate limiting is fail-closed when bindings are missing", async () => {
  const env = makeEnv();
  delete env.DEVICE_RATE_LIMITER;
  const response = await worker.fetch(request("/credits"), env);
  assert.equal(response.status, 503);
});

test("rate limiting rejects requests when a binding denies them", async () => {
  const env = makeEnv();
  env.DEVICE_RATE_LIMITER = { limit: async () => ({ success: false }) };
  const response = await worker.fetch(request("/credits"), env);
  assert.equal(response.status, 429);
  assert.equal(response.headers.get("retry-after"), "60");
});

test("upstream model failures return 502 and refund the reserved credit", async () => {
  const env = makeEnv({ limit: 2, aiRun: async () => { throw new Error("upstream failure"); } });
  const response = await worker.fetch(request("/generate", { prompt: "test" }), env);
  assert.equal(response.status, 502);
  const credits = await worker.fetch(request("/credits"), env);
  assert.deepEqual(await credits.json(), { credits: 2, limit: 2 });
});
