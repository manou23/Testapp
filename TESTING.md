# Mesut AI Studio — Test checklist

A successful build confirms compilation only. It does not prove deployed AI inference, account quotas, ad rewards, purchases, or device-specific behavior.

## Automated Worker tests

Run locally from backend/ with Node.js 22 using npm test. GitHub Actions runs these tests before the Android job.

Covered cases:
- Missing or malformed installation identifier.
- Starting daily balance.
- FLUX.1 schnell base64 image response decoding.
- FLUX.2 klein 4B multipart edit request shape.
- Invalid edit payload rejected without consuming a credit.
- Daily quota blocks a subsequent inference.
- Missing rate-limit bindings fail closed.
- Upstream model errors return HTTP 502.

## Android manual checks

- [ ] Install and launch on Android 8.0 (API 26) or later.
- [ ] Splash, Home, Create, Edit, Creations, Favorites, Styles, Premium and Settings open.
- [ ] Arabic, French and English labels display correctly.
- [ ] RTL/LTR layouts are correct.
- [ ] Empty prompts are rejected.
- [ ] Generate a real image against a deployed Worker with valid AI binding.
- [ ] Edit a real image against the configured FLUX.2 klein 4B model.
- [ ] Verify model cost/usage in Cloudflare before enabling public access.
- [ ] Remove background locally; verify transparent PNG output.
- [ ] Confirm 2x resize is standard interpolation, not AI enhancement.
- [ ] Generate 1–4 images and verify completed outputs remain if a later request is denied.
- [ ] Save, share, favorite, delete one image and delete all images.
- [ ] Verify offline, HTTP 429, HTTP 502 and HTTP 503 messages.
- [ ] Test on a device without Google Play services; local ML Kit subject segmentation may be unavailable.

## Backend deployment checks

- [ ] Replace the KV namespace placeholder in backend/wrangler.toml.
- [ ] Deploy the Worker with AI, LIMITS, DEVICE_RATE_LIMITER and IP_RATE_LIMITER bindings.
- [ ] Set GitHub Actions variable BACKEND_URL.
- [ ] Verify requests without a valid device identifier are rejected.
- [ ] Verify device and IP rate limiting.
- [ ] Verify daily credits decrement once per successful request.
- [ ] Confirm no provider credential is present in the APK or repository.
- [ ] Keep /reward disabled until AdMob SSV signature verification is implemented.

## Release and store checks

- [ ] Publish a completed privacy policy at a stable URL.
- [ ] Review Cloudflare model pricing, quota and data-processing terms.
- [ ] Configure real AdMob app/ad unit IDs and consent requirements before production ads.
- [ ] Implement and test AdMob SSV before awarding rewarded credits.
- [ ] Configure Google Play Billing and server-side purchase verification before Premium is sold.
- [ ] Back up the permanent signing key securely.
- [ ] Test upgrade installation using the same signing key.
- [ ] Complete Play Console data safety, content rating, target API, screenshots and listing requirements.

## Known limitations in 1.0.2

- Worker daily quotas use KV read/modify/write and are not globally atomic.
- Cloudflare rate-limit bindings are approximate and can exceed configured thresholds briefly.
- Installation IDs can be reset; no user-account authentication exists.
- Rewarded ad credit awards and Premium purchases are not enabled.
- AI super-resolution, object removal/inpainting and background replacement are not implemented.
- Production signing is not possible until the owner configures a permanent keystore.
- Real-device and deployed-Worker tests remain required.
