const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store"
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: JSON_HEADERS });
}

function deviceKey(request) {
  const id = (request.headers.get("X-Device-Id") || "").trim();
  if (!/^[a-zA-Z0-9_-]{12,80}$/.test(id)) return null;
  return id;
}

function dateKey() {
  return new Date().toISOString().slice(0, 10);
}

async function usage(env, device) {
  if (!env.LIMITS) throw new Error("KV binding LIMITS is not configured");
  const key = "usage:" + dateKey() + ":" + device;
  const count = Number(await env.LIMITS.get(key) || "0");
  return { key, count };
}

async function consume(env, device) {
  const current = await usage(env, device);
  const limit = Math.max(1, Number(env.DAILY_FREE_LIMIT || 3));
  if (current.count >= limit) return false;
  await env.LIMITS.put(current.key, String(current.count + 1), { expirationTtl: 60 * 60 * 48 });
  return true;
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
  if (result instanceof Uint8Array) return result;
  if (result instanceof ArrayBuffer) return new Uint8Array(result);
  if (result && result.image instanceof Uint8Array) return result.image;
  if (result && result.image instanceof ArrayBuffer) return new Uint8Array(result.image);
  throw new Error("AI provider returned an unsupported image response");
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
  const chosen = styles[Math.max(0, Math.min(styles.length - 1, Number(style) || 0))];
  return prompt + ", " + chosen;
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

      if (url.pathname === "/credits" && request.method === "GET") {
        const state = await usage(env, device);
        const limit = Math.max(1, Number(env.DAILY_FREE_LIMIT || 3));
        return json({ credits: Math.max(0, limit - state.count), limit });
      }

      if (url.pathname === "/reward" && request.method === "POST") {
        // Never trust the app client to award credits. Add an authenticated AdMob SSV
        // callback and verify its signature before enabling reward claims.
        return json({ error: "Reward verification is not configured" }, 501);
      }

      if (url.pathname !== "/generate" && url.pathname !== "/edit") {
        return json({ error: "Not found" }, 404);
      }
      if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
      if (!env.AI || typeof env.AI.run !== "function") {
        return json({ error: "Cloudflare AI binding is not configured" }, 503);
      }

      const raw = await request.text();
      if (raw.length > 14_000_000) return json({ error: "Request is too large" }, 413);
      let body;
      try { body = JSON.parse(raw); } catch { return json({ error: "Invalid JSON" }, 400); }

      const prompt = typeof body.prompt === "string" ? body.prompt.trim() : "";
      if (!prompt || prompt.length > 2000) return json({ error: "Prompt must contain 1 to 2000 characters" }, 400);

      const allowed = await consume(env, device);
      if (!allowed) return json({ error: "Daily credit limit reached" }, 429);

      let result;
      if (url.pathname === "/generate") {
        result = await env.AI.run("@cf/black-forest-labs/flux-1-schnell", {
          prompt: stylePrompt(prompt, body.style),
          num_steps: 4
        });
      } else {
        if (typeof body.image !== "string" || body.image.length > 12_000_000) {
          return json({ error: "A valid image is required" }, 400);
        }
        let binary;
        try { binary = atob(body.image); } catch { return json({ error: "Invalid image encoding" }, 400); }
        const image = Uint8Array.from(binary, c => c.charCodeAt(0));
        result = await env.AI.run("@cf/black-forest-labs/flux-1-kontext-dev", {
          prompt,
          image
        });
      }

      const bytes = await imageBytes(result);
      return json({ image: toBase64(bytes) });
    } catch (error) {
      console.error("Mesut AI Worker error:", error);
      return json({ error: "The AI service is temporarily unavailable" }, 500);
    }
  }
};