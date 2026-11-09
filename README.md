# ✨ Mesut AI Studio

<div align="center">

**استوديو إبداعي لإنشاء الصور وتعديلها بالذكاء الاصطناعي**

*Create beyond imagination.*

![Version](https://img.shields.io/badge/version-1.0.4-7C3AED?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Build](https://img.shields.io/badge/build-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

</div>

---

## 📥 Download Mesut AI Studio

<div align="center">

### النسخة الأحدث للأندرويد

<a href="https://github.com/manou23/Testapp/releases/download/latest-apk/Mesut-AI-Studio.apk">
  <img src="https://img.shields.io/badge/⬇%20تحميل%20مباشر-APK-7C3AED?style=for-the-badge&logo=android&logoColor=white" alt="Download Mesut AI Studio APK">
</a>

**الإصدار الحالي: 1.0.4** · Android 8.0 أو أحدث

[تحميل مباشر APK](https://github.com/manou23/Testapp/releases/download/latest-apk/Mesut-AI-Studio.apk) · [عرض جميع الإصدارات](https://github.com/manou23/Testapp/releases)

</div>

### طريقة التثبيت

1. اضغط زر **تحميل مباشر APK** أعلاه.
2. انتظر اكتمال تنزيل الملف على هاتف Android.
3. افتح الملف الذي تم تنزيله، واسمح بالتثبيت من هذا المصدر إذا طلب Android ذلك.
4. اضغط **تثبيت** ثم افتح التطبيق.

### تحديثات التطبيق

كلما ينجح بناء نسخة جديدة على GitHub Actions، يُحدَّث رابط التحميل المباشر نفسه تلقائياً ليشير إلى أحدث APK. يمكنك أيضاً متابعة [حالة البناء](https://github.com/manou23/Testapp/actions).

> **ملاحظة مهمة:** الملف الحالي APK تجريبي موقّع بمفتاح Debug. إذا كان لديك إصدار سابق موقّع بمفتاح مختلف، فقد يطلب Android إزالة الإصدار القديم قبل تثبيت الجديد. لا تحذف إصدارك الحالي إذا كنت تريد الاحتفاظ ببياناته قبل التأكد من النسخ الاحتياطي. إصدار الإنتاج الموقّع بمفتاح دائم يحتاج إعداد مفتاح التوقيع الخاص بالمشروع.

## 🌟 Features in 1.0.4

- Text-to-image generation through Cloudflare Workers AI FLUX.2 klein 4B, with automatic Arabic-to-English prompt translation.
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

- **Zero-budget setup is possible within Cloudflare's Free allocation, but not unlimited.** Workers AI currently includes 10,000 Neurons per day at no charge on the Free plan. When that allowance is exhausted, further inference fails until the daily reset; it does not silently become unlimited. FLUX.2 klein 4B consumes priced Neurons per input/output tile. Do not upgrade to Workers Paid if you must keep spending at zero. Review the [official pricing and free allocation](https://developers.cloudflare.com/workers-ai/platform/pricing/) before launch.
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

GitHub Actions runs Node.js Worker tests first, then Android JUnit unit tests, and only when both pass does it build the Android debug APK.

Local requirements:
- JDK 17
- Gradle 8.13
- Android SDK for API 36

```bash
gradle --no-daemon clean assembleDebug -PbackendUrl="https://YOUR-WORKER.workers.dev"
```

Output: `app/build/outputs/apk/debug/`.

## ☁️ Deploy Cloudflare Worker

The backend uses the Workers AI binding, a KV namespace, and two approximate Rate Limiting bindings. Deployment is automated by **Deploy Cloudflare Worker** in GitHub Actions.

### One-time Cloudflare setup

1. In Cloudflare Dashboard → Workers & Pages → KV, create a namespace named `MESUT_AI_LIMITS` and copy its 32-character namespace ID.
2. Create an account-scoped Cloudflare API token for CI. Because this workflow may create the Worker on its first deployment, grant **Workers Admin** at the Workers product scope; after the Worker exists, rotate to a token scoped to **Workers Editor** for that Worker. Add **Account Settings: Read** if Wrangler requests account metadata access. KV is created manually, so do not grant KV write unless you also want the pipeline to manage KV resources. Never paste this token into source code or chat.
3. In GitHub → Settings → Secrets and variables → Actions → Secrets, add:
   - `CLOUDFLARE_API_TOKEN`
   - `CLOUDFLARE_ACCOUNT_ID`
   - `CLOUDFLARE_KV_NAMESPACE_ID`
4. Open Actions → **Deploy Cloudflare Worker** → Run workflow.
5. The workflow deploys the Worker, saves the detected HTTPS URL as the repository variable `BACKEND_URL`, then triggers a new Android build using that URL.

The workflow substitutes the KV ID only in its temporary runner checkout; it never commits the account-specific namespace ID to the public repository. Do not add the KV ID or API token to Android code.

If the deployment cannot detect a `workers.dev` URL, enable the workers.dev subdomain for the account and run the workflow again.

Required bindings:
- `AI`: Cloudflare Workers AI.
- `LIMITS`: KV namespace for daily counters.
- `DEVICE_RATE_LIMITER`: 12 requests per 60 seconds per installation ID.
- `IP_RATE_LIMITER`: 60 requests per 60 seconds per source IP. The configured namespace IDs `1001` and `1002` must be unique within your Cloudflare account; change them if another Worker already uses either ID. Cloudflare rate limiting is approximate and location-local, not a strict global quota.

The Worker exposes:
- `GET /credits`
- `POST /generate`
- `POST /edit`
- `POST /reward` (intentionally disabled with HTTP 501 until SSV verification exists)

The Android build now fails early if `BACKEND_URL` is missing, instead of packaging an APK that cannot reach the AI service.

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

The application ID remains `com.mesut23.aistudio`. Increase `versionCode` for every release and sign all production updates with the same permanent key. The Release workflow builds both a signed APK and a signed AAB; use the AAB for a new Google Play listing because Play requires Android App Bundles for new apps.

## 📢 Ads, Premium and privacy

The Android project uses Google's test AdMob application/banner identifiers during development. Replace them only after configuring the production AdMob account and required consent flow. Rewarded credits must not be granted by trusting the app client.

Premium is currently an informational, disabled screen. Do not advertise or sell Premium until Play Billing and server-side purchase verification are working.

Review and complete [PRIVACY_POLICY_DRAFT_AR.md](PRIVACY_POLICY_DRAFT_AR.md) and [PRIVACY_POLICY_TEMPLATE.md](PRIVACY_POLICY_TEMPLATE.md) before publication. The draft contains owner/contact placeholders and must not be published unchanged. The policy must accurately describe the deployed Worker, Cloudflare model processing, retention, ads, and user contact details.

## 🧪 Tests

Run Worker unit tests:

```bash
cd backend
npm test
```

Automated tests cover input validation, image response decoding, model request shape, quota behavior, rate-limit configuration, upstream failures, MIME mapping and aspect-ratio sanity. Android UI, real Cloudflare inference, AdMob, billing and physical-device behavior still require manual testing.

## 📄 License

No open-source license has been selected. All rights reserved by the project owner unless a license is added.

---

<div align="center">

**MESUT AI STUDIO**  
*Built with creativity.*

</div>
