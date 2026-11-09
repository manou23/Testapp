# ✨ Mesut AI Studio

<div align="center">

**استوديو إبداعي لإنشاء الصور وتعديلها بمساعدة الذكاء الاصطناعي**

*Create beyond imagination.*

![Version](https://img.shields.io/badge/version-1.0.1-7C3AED?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Build](https://img.shields.io/badge/build-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

</div>

---

## 📥 Download

**Current source version: 1.0.1**

Version 1.0.1 is the first patch update. It fixes a Kotlin compilation issue and retains the newer app/backend implementation after reviewing the supplied project archive.

The APK is not published yet. A debug APK is generated as a GitHub Actions artifact after a successful build. A public download link will be added to [Releases](https://github.com/manou23/Testapp/releases) only after the app has been built, configured and tested.

[![Open latest build](https://img.shields.io/badge/Open-GitHub_Actions_Builds-24292F?style=for-the-badge&logo=github)](https://github.com/manou23/Testapp/actions)

## 🌟 About

Mesut AI Studio is an Android creative workspace built with Kotlin and Jetpack Compose. The application includes prompt-based image generation, AI-assisted image editing, a creations gallery, favorites, image saving/sharing, and Arabic, English and French interface resources.

The Android app communicates with a separate Cloudflare Worker. Provider credentials are intended to remain on the server and must never be embedded in the APK.

## ✨ App features

- **AI image generation** from a text prompt.
- **AI image editing** from a selected photo and an instruction.
- **Aspect ratios** including 1:1, 9:16, 16:9 and 4:3.
- **Style presets** for different visual directions.
- **Creations gallery** with favorite, share and delete actions.
- **Save to device gallery** and share generated images.
- **Arabic, English and French** interface translations.
- **Dark visual design** based on Jetpack Compose.

Some services require a deployed and configured backend. Premium purchases and rewarded-credit verification are not enabled in this release.

## 🧩 Project structure

```
.
├── app/                         # Android application
│   └── src/main/
│       ├── java/com/mesut23/aistudio/
│       │   ├── MainActivity.kt
│       │   ├── UI.kt
│       │   └── Core.kt
│       ├── res/                 # Localized strings and app resources
│       └── AndroidManifest.xml
├── backend/
│   ├── src/index.js             # Cloudflare Worker API
│   └── wrangler.toml
├── .github/workflows/build.yml  # Automated APK build
├── VERSION
├── CHANGELOG.md
└── README.md
```

## 🛠️ Build the Android app

The repository uses GitHub Actions:

1. Open the [Actions page](https://github.com/manou23/Testapp/actions).
2. Select **Build Android APK**.
3. Open the latest successful run.
4. Download the artifact named **Mesut-AI-Studio-1.0.1-debug**.

For local builds, use JDK 17 and Gradle 8.9:

```bash
gradle --no-daemon assembleDebug -PbackendUrl="https://YOUR-WORKER.workers.dev"
```

The APK is written to `app/build/outputs/apk/debug/`.

## ☁️ Backend setup

The backend uses Cloudflare Workers AI and a KV namespace for per-device daily usage.

1. Create a Cloudflare Worker and a KV namespace.
2. Replace `REPLACE_WITH_KV_NAMESPACE_ID` in `backend/wrangler.toml` with the namespace ID.
3. Deploy from the `backend/` directory with Wrangler.
4. Set the GitHub Actions repository variable `BACKEND_URL` to the deployed Worker URL.

Example:

```bash
cd backend
npx wrangler deploy
```

The Worker expects the `AI` binding and `LIMITS` KV binding configured in `wrangler.toml`. Review Cloudflare model availability and account limits before deployment.

**Important:** The `/reward` endpoint intentionally refuses client-side reward claims. Do not enable rewarded credits until an authenticated AdMob Server-Side Verification (SSV) callback has been implemented and verified.

## 🔐 Security and release readiness

- Do not commit API keys, passwords, keystores or signing credentials.
- Keep AI provider access on the backend, never inside the Android app.
- Configure rate limits and monitor Worker usage.
- Rewarded ads require server-side verification.
- Review image retention, privacy disclosures and model licenses before public release.
- Premium billing is not implemented; the Premium screen is informational only.

## 🔄 Versioning and safe updates

Release sequence:

| Release | Version | versionCode |
|---|---:|---:|
| First release | 1.0.0 | 1 |
| First patch | 1.0.1 | 2 |
| Next patch | 1.0.2 | 3 |

For Android to install a new release over the previous installation, keep the same `applicationId`, sign every release with the same private signing key, and increase `versionCode` every time. A debug APK is not a substitute for a properly signed production release.

For the Release workflow, configure these GitHub repository secrets:

- `ANDROID_KEYSTORE_BASE64` — Base64-encoded private `.jks` keystore.
- `ANDROID_KEYSTORE_PASSWORD` — keystore password.
- `ANDROID_KEY_ALIAS` — key alias.
- `ANDROID_KEY_PASSWORD` — key password.

Also configure the repository variable `BACKEND_URL`. Keep the original keystore backed up securely: losing it prevents signing compatible updates. Never paste these secrets into chat or commit them to Git.

## 🧪 Testing

See [TESTING.md](TESTING.md) for the manual test checklist. See [CHANGELOG.md](CHANGELOG.md) for release history.

## 📄 License

No open-source license has been selected yet. All rights reserved by the project owner unless a license is added.

---

<div align="center">

**MESUT AI STUDIO**  
*Built with creativity.*

</div>
