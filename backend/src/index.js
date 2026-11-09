const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store"
};

const GENERATION_MODEL = "@cf/black-forest-labs/flux-1-schnell";
const EDIT_MODEL = "@cf/black-forest-labs/flux-2-klein-4b";
const MAX_PROMPT_LENGTH = 2000;
const MAX_IMAGE_BASE64_LENGTH = 5_600_000;
const MAX_REQUEST_LENGTH = 6_000_000;

function json(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...JSON_HEADERS, ...extraHeaders }
  });
}

function deviceKey(request) {
  const id = (request.headers.get("X-Device-Id") || "").trim();
  return /^[a-zA-Z0-9_-]{12,80}$/.test(id) ? id : null;
}

function dateKey() {
  return new Date().toISOString().slice(0, 10);
}

async function usage(env, device) {
  if (!env.LIMITS) throw new Error("KV binding LIMITS is not configured");
  const key = "usage:" + dateKey() + ":" + device;
  const count = Number(await env.LIMITS.get(key) || "0");
  return { key, count: Number.isFinite(count) ? count : 0 };
}

async function consume(env, device) {
  const current = await usage(env, device);
  const limit = Math.max(1, Number(env.DAILY_FREE_LIMIT || 3));
  if (current.count >= limit) return false;
  await env.LIMITS.put(current.key, String(current.count + 1), {
    expirationTtl: 60 * 60 * 48
  });
  return true;
}

async function refund(env, device) {
  const current = await usage(env, device);
  if (current.count > 0) {
    await env.LIMITS.put(current.key, String(current.count - 1), {
      expirationTtl: 60 * 60 * 48
    });
  }
}

async function enforceRateLimits(request, env, device) {
  if (!env.DEVICE_RATE_LIMITER || !env.IP_RATE_LIMITER) {
    return { allowed: false, reason: "rate_limit_not_configured" };
  }
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  const [deviceResult, ipResult] = await Promise.all([
    env.DEVICE_RATE_LIMITER.limit({ key: device }),
    env.IP_RATE_LIMITER.limit({ key: ip })
  ]);
  return {
    allowed: deviceResult.success && ipResult.success,
    reason: "rate_limited"
  };
}

function decodeBase64(value) {
  try {
    const binary = atob(value);
    return Uint8Array.from(binary, character => character.charCodeAt(0));
  } catch {
    return null;
  }
}

function toBase64(bytes) {
  let binary = "";
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}

async function imageBytes(result) {
  if (typeof result === "string") {
    const decoded = decodeBase64(result);
    if (decoded) return decoded;
  }
  if (result instanceof Uint8Array) return result;
  if (result instanceof ArrayBuffer) return new Uint8Array(result);
  if (result && typeof result.image === "string") {
    const decoded = decodeBase64(result.image);
    if (decoded) return decoded;
  }
  if (result && result.image instanceof Uint8Array) return result.image;
  if (result && result.image instanceof ArrayBuffer) return new Uint8Array(result.image);
  throw new Error("AI provider returned an unsupported image response");
}

function detectImageMime(bytes) {
  if (bytes.length >= 8 &&
      bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e &&
      bytes[3] === 0x47 && bytes[4] === 0x0d && bytes[5] === 0x0a &&
      bytes[6] === 0x1a && bytes[7] === 0x0a) return "image/png";
  if (bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
    return "image/jpeg";
  }
  throw new Error("AI provider returned an unrecognized image format");
}

function decodeSupportedImage(value) {
  const bytes = decodeBase64(value);
  if (!bytes || bytes.length === 0) return null;
  try {
    return { bytes, mimeType: detectImageMime(bytes) };
  } catch {
    return null;
  }
}

function stylePrompt(prompt, style) {
  const styles = [
    "photorealistic, highly detailed",
    "cinematic lighting, dramatic composition",
    "anime illustration",
    "high quality 3D render",
    "digital concept art",
    "watercolor painting",
    "oil painting on canvas",
    "fantasy art, imaginative atmosphere"
  ];
  const index = Number.isInteger(Number(style)) ? Number(style) : 0;
  const chosen = styles[Math.max(0, Math.min(styles.length - 1, index))];
  return prompt + ", " + chosen;
}

async function readJson(request) {
  const raw = await request.text();
  if (raw.length > MAX_REQUEST_LENGTH) {
    return { error: json({ error: "Request is too large" }, 413) };
  }
  try {
    return { body: JSON.parse(raw) };
  } catch {
    return { error: json({ error: "Invalid JSON" }, 400) };
  }
}

async function generate(env, prompt, style) {
  return env.AI.run(GENERATION_MODEL, {
    prompt: stylePrompt(prompt, style),
    steps: 4
  });
}

async function edit(env, prompt, base64Image) {
  const decoded = decodeSupportedImage(base64Image);
  if (!decoded) throw new TypeError("INVALID_IMAGE");

  const extension = decoded.mimeType === "image/png" ? "png" : "jpg";
  const form = new FormData();
  form.append("prompt", prompt);
  form.append("input_image_0", new Blob([decoded.bytes], { type: decoded.mimeType }), "input." + extension);
  const serialized = new Response(form);
  const contentType = serialized.headers.get("content-type");
  if (!contentType || !serialized.body) throw new Error("Could not create AI image payload");

  return env.AI.run(EDIT_MODEL, {
    multipart: {
      body: serialized.body,
      contentType
    }
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "access-control-allow-origin": "*",
          "access-control-allow-methods": "GET, POST, OPTIONS",
          "access-control-allow-headers": "Content-Type, X-Device-Id",
          "access-control-max-age": "86400"
        }
      });
    }

    try {
      const device = deviceKey(request);
      if (!device) return json({ error: "A valid X-Device-Id header is required" }, 400);

      const rate = await enforceRateLimits(request, env, device);
      if (!rate.allowed) {
        const status = rate.reason === "rate_limit_not_configured" ? 503 : 429;
        return json({
          error: status === 503 ? "Service protection is not configured" : "Too many requests. Try again shortly."
        }, status, status === 429 ? { "retry-after": "60" } : {});
      }

      if (url.pathname === "/credits" && request.method === "GET") {
        const state = await usage(env, device);
        const limit = Math.max(1, Number(env.DAILY_FREE_LIMIT || 3));
        return json({ credits: Math.max(0, limit - state.count), limit });
      }

      if (url.pathname === "/reward" && request.method === "POST") {
        return json({ error: "Reward verification is not configured" }, 501);
      }

      if (url.pathname !== "/generate" && url.pathname !== "/edit") {
        return json({ error: "Not found" }, 404);
      }
      if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
      if (!env.AI || typeof env.AI.run !== "function") {
        return json({ error: "Cloudflare AI binding is not configured" }, 503);
      }

      const parsed = await readJson(request);
      if (parsed.error) return parsed.error;
      const body = parsed.body;
      const prompt = typeof body.prompt === "string" ? body.prompt.trim() : "";
      if (!prompt || prompt.length > MAX_PROMPT_LENGTH) {
        return json({ error: "Prompt must contain 1 to 2000 characters" }, 400);
      }

      if (url.pathname === "/edit") {
        if (typeof body.image !== "string" || body.image.length === 0 ||
            body.image.length > MAX_IMAGE_BASE64_LENGTH || !decodeSupportedImage(body.image)) {
          return json({ error: "A valid JPEG or PNG image is required" }, 400);
        }
      }

      const allowed = await consume(env, device);
      if (!allowed) return json({ error: "Daily credit limit reached" }, 429);

      try {
        const result = url.pathname === "/generate"
          ? await generate(env, prompt, body.style)
          : await edit(env, prompt, body.image);
        const bytes = await imageBytes(result);
        const mimeType = detectImageMime(bytes);
        return json({ image: toBase64(bytes), mimeType });
      } catch (error) {
        await refund(env, device);
        console.error("Workers AI inference failed:", error);
        return json({ error: "The AI model could not process this request" }, 502);
      }
    } catch (error) {
      console.error("Mesut AI Worker error:", error);
      return json({ error: "The AI service is temporarily unavailable" }, 500);
    }
  }
};
