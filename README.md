# ✨ Mesut AI Studio

<div align="center">

**استوديو إبداعي لإنشاء الصور وتعديلها بالذكاء الاصطناعي**

*Create beyond imagination.*

![Version](https://img.shields.io/badge/version-1.0.2-7C3AED?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Build](https://img.shields.io/badge/build-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

</div>

---

## 📥 Download APK

**Source version: 1.0.2**

1. Open [GitHub Actions](https://github.com/manou23/Testapp/actions).
2. Open the latest successful **Build Android APK** workflow run.
3. Download the artifact named **Mesut-AI-Studio-1.0.2-debug**.
4. Extract the ZIP and install the APK.

The workflow builds a **debug APK** for testing. It is not a production-signed APK and may not update an APK signed with a different key. A permanent signed release requires the owner's private keystore.

## 🌟 Features in 1.0.2

- Text-to-image generation through Cloudflare Workers AI FLUX.1 schnell.
- Prompt-guided image editing through FLUX.2 klein 4B with a reference image.
- 1–4 sequential image variations, with completed images retained if later credits run out.
- Local subject cut-out using ML Kit, saved as transparent PNG.
- Standard local 2× bitmap resizing. This is **not** AI super-resolution.
- Style presets and aspect ratios.
- Local creations history, favorites, save-to-gallery and Android sharing.
- Daily server-side free balance and Cloudflare request throttling.
- Arabic default interface, plus French and English resources.
- Dark Jetpack Compose interface with a startup splash.

## ⚠️ Honest feature and cost status

This is an actively developed MVP, not a claim that every commercial editor feature is already available.

- **AI inference is not guaranteed to be free or unlimited.** Cloudflare quotas and prices depend on the account and model. FLUX.2 klein 4B is a priced partner model; review the [Cloudflare Workers AI pricing](https://developers.cloudflare.com/workers-ai/platform/pricing/) before enabling public editing.
- **Rewarded AdMob credits are disabled** until a verified Google AdMob Server-Side Verification (SSV) callback is implemented.
- **Premium purchase and ad removal are disabled** until Google Play Billing and server-side purchase verification are configured.
- AI super-resolution, object removal/inpainting and AI background replacement are not implemented.
- Local ML Kit background removal requires Google Play services and its downloadable model.
- Daily KV counters are not globally atomic; Cloudflare Rate Limiting is approximate. These are first-line abuse controls, not fraud-proof identity or strict global quotas.

## 🧩 Project structure

```
.
├── app/
│   └── src/main/
│       ├── java/com/mesut23/aistudio/
│       │   ├── MainActivity.kt
│       │   ├── UI.kt
│       │   └── Core.kt
│       ├── res/
│       └── AndroidManifest.xml
├── backend/
│   ├── src/index.js
│   ├── src/index.test.js
│   ├── package.json
│   └── wrangler.toml
├── .github/workflows/
│   ├── build.yml
│   └── release.yml
├── ARCHITECTURE.md
├── TESTING.md
├── CHANGELOG.md
└── VERSION
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for the MVVM and Worker design, and [TESTING.md](TESTING.md) for automated and manual checks.

## 🛠️ Build Android APK

GitHub Actions runs Node.js Worker tests first. Only when they pass does it build the Android debug APK.

Local requirements:
- JDK 17
- Gradle 8.9
- Android SDK for API 35

```bash
gradle --no-daemon clean assembleDebug -PbackendUrl="https://YOUR-WORKER.workers.dev"
```

Output: `app/build/outputs/apk/debug/`.

## ☁️ Deploy Cloudflare Worker

The backend uses the Workers AI binding, a KV namespace, and two Rate Limiting bindings.

1. Create a Cloudflare KV namespace.
2. Replace `REPLACE_WITH_KV_NAMESPACE_ID` in `backend/wrangler.toml`.
3. Install/use Wrangler 4.36.0 or later.
4. From `backend/`, deploy the Worker.
5. Set the GitHub repository variable `BACKEND_URL` to the deployed Worker URL.

```bash
cd backend
npx wrangler@latest deploy
```

Required bindings:
- `AI`: Cloudflare Workers AI.
- `LIMITS`: KV namespace for daily counters.
- `DEVICE_RATE_LIMITER`: 12 requests per 60 seconds per installation ID.
- `IP_RATE_LIMITER`: 60 requests per 60 seconds per source IP.

The Worker exposes:
- `GET /credits`
- `POST /generate`
- `POST /edit`
- `POST /reward` (intentionally disabled with HTTP 501 until SSV verification exists)

No Cloudflare API token or model provider key is placed in the Android application. Keep account credentials in Cloudflare/GitHub settings only.

## 🔐 Release signing

Configure these GitHub repository secrets for the signed Release workflow:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Also configure the `BACKEND_URL` repository variable. Keep the original keystore backed up securely; losing it prevents compatible updates. Never commit the keystore or paste passwords into chat.

The application ID remains `com.mesut23.aistudio`. Increase `versionCode` for every release and sign all production updates with the same permanent key.

## 📢 Ads, Premium and privacy

The Android project uses Google's test AdMob application/banner identifiers during development. Replace them only after configuring the production AdMob account and required consent flow. Rewarded credits must not be granted by trusting the app client.

Premium is currently an informational, disabled screen. Do not advertise or sell Premium until Play Billing and server-side purchase verification are working.

Review and complete [PRIVACY_POLICY_TEMPLATE.md](PRIVACY_POLICY_TEMPLATE.md) before publication. The policy must accurately describe the deployed Worker, Cloudflare model processing, retention, ads, and user contact details.

## 🧪 Tests

Run Worker unit tests:

```bash
cd backend
npm test
```

Automated tests cover input validation, image response decoding, model request shape, quota behavior, rate-limit configuration and upstream failures. Android UI, real Cloudflare inference, AdMob, billing and physical-device behavior still require manual testing.

## 📄 License

No open-source license has been selected. All rights reserved by the project owner unless a license is added.

---

<div align="center">

**MESUT AI STUDIO**  
*Built with creativity.*

</div>
