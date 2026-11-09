# Mesut AI Studio — Architecture and MVP

## Product scope (1.0.2)

Android 8.0+ creative image studio built with Kotlin and Jetpack Compose. Arabic is the default language, with French and English resources and Android RTL/LTR layout support.

### MVP capabilities

- Text-to-image generation through Cloudflare Workers AI FLUX.1 schnell.
- Prompt-guided image editing through FLUX.2 klein 4B with a reference image.
- On-device subject cut-out with ML Kit, saved as transparent PNG.
- Local 2x bitmap resize. This is conventional interpolation, not AI super-resolution.
- Preset styles, aspect ratios, and 1–4 sequential variations.
- Local creations history, favorites, save-to-gallery and Android share sheet.
- Server-controlled daily credits and request throttling.
- AdMob test banner during development.

### Explicitly unavailable

- Rewarded AdMob credit awards: disabled until Google SSV signature verification is deployed.
- Premium purchases and ad removal: disabled until Play Billing products and server-side purchase verification are configured.
- AI super-resolution, object removal/inpainting, and background replacement.
- Unlimited AI inference. Cloudflare Workers AI currently includes 10,000 Neurons per day on the Free plan; when exhausted, further inference stops until reset. FLUX.2 klein 4B consumes Neurons per input/output tile. Keeping the account on the Free plan avoids paid overage but imposes the shared daily cap.

## Architecture

### Android (MVVM)

- MainActivity.kt: activity entry point and system window setup.
- UI.kt: Compose navigation, screen components, localization, theme, splash.
- Core.kt: ViewModel, AI provider interface, HTTP proxy, local image processing, favorites and file storage.
- res/values*: Arabic, French and English UI strings.
- AndroidManifest.xml: internet, AdMob test application ID, RTL and FileProvider.

The existing source is kept in place for compatibility. The next refactor can split Core.kt and UI.kt into data/, domain/, and ui/ packages without changing the application ID.

### Cloudflare Worker

- GET /credits: returns server-side daily free balance.
- POST /generate: calls FLUX.1 schnell.
- POST /edit: calls FLUX.2 klein 4B using multipart reference-image input.
- POST /reward: deliberately returns 501 until authenticated AdMob SSV verification is implemented.

The Worker owns the AI binding and never sends provider credentials to the Android app. The app sends a random installation identifier, prompt, and (for editing) a compressed image.

### Usage and abuse controls

- Daily usage is stored in Cloudflare KV.
- Cloudflare Rate Limiting bindings throttle each installation and source IP.
- These controls are a first-line abuse barrier, not a strict global quota: Cloudflare's rate limiter is approximate and KV read/modify/write is not an atomic transaction. A Durable Object would be needed for a strict globally serialized allowance.
- The installation identifier is not an authenticated account identity and can be reset by a determined attacker. Do not treat it as proof of a person or a payment.

## Build and release

- GitHub Actions runs Worker unit tests before building the Android debug APK.
- BACKEND_URL is a GitHub Actions repository variable.
- Release signing requires a private keystore and four GitHub Actions secrets. Never commit the keystore.
- Keep applicationId unchanged and increase versionCode for every Play Store update.

## Test status

The Worker has Node.js unit tests for request validation, base64 image decoding, model routing, quota enforcement, missing rate-limit bindings, and upstream failures. Android has JUnit tests for MIME mapping and aspect-ratio sanity. GitHub Actions is the authoritative CI result.

A successful APK build verifies compilation, not actual model availability, Cloudflare account billing, a deployed Worker, AdMob SSV, Play Billing, Play Store policy compliance, or behavior on a physical device.
