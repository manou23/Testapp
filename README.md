# ✨ Mesut AI Studio

<p align="center">
  <strong>استوديو الذكاء الاصطناعي — أنشئ، ابتكر، وأطلق خيالك.</strong><br/>
  <em>Create beyond imagination.</em>
</p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-1.0.0-7c3aed?style=for-the-badge"/>
  <img alt="Android" src="https://img.shields.io/badge/Android-Kotlin-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img alt="Status" src="https://img.shields.io/badge/status-preparing-ffb020?style=for-the-badge"/>
</p>

---

## 📲 تحميل التطبيق

<div align="center">

### Mesut AI Studio · v1.0.0

**سيظهر رابط APK هنا فور نجاح البناء ونشر الإصدار.**

[![Download APK](https://img.shields.io/badge/Download-APK-6D28D9?style=for-the-badge&logo=android&logoColor=white)](../../releases/latest)

</div>

> رابط التحميل أعلاه يفتح صفحة أحدث إصدار في GitHub Releases. لن يكون هناك ملف APK قابل للتنزيل حتى يتم بناء التطبيق ونشره فعلياً.

## 🚀 نبذة عن المشروع

Mesut AI Studio تطبيق Android لإنشاء الصور وتحريرها بالذكاء الاصطناعي، بواجهة حديثة ودعم للغة العربية واللغات الإضافية. يتكون المشروع من تطبيق الهاتف وخدمة خلفية مستقلة.

## ✨ الوظائف الموجودة في المشروع المصدر

- إنشاء الصور انطلاقاً من وصف نصي.
- تعديل الصور باستخدام الذكاء الاصطناعي.
- معرض للأعمال المنشأة.
- إدارة رصيد الاستخدام والمكافآت.
- واجهة Android مبنية بـ Kotlin وJetpack Compose.
- خدمة خلفية Cloudflare Worker للاتصال بنماذج الذكاء الاصطناعي.

## 🧱 البنية التقنية

| المكوّن | التقنية |
|---|---|
| Android | Kotlin |
| UI | Jetpack Compose |
| Build | Gradle |
| Backend | Cloudflare Workers |
| AI | Cloudflare Workers AI |

## 🗂️ بنية المستودع

- `app/` — تطبيق Android.
- `backend/` — الخدمة الخلفية.
- `.github/workflows/` — سير عمل البناء الآلي.
- `VERSION` — رقم الإصدار الحالي.
- `CHANGELOG.md` — سجل التغييرات.

## 🔢 نظام الإصدارات والتحديث

نعتمد ترقيم الإصدارات بالشكل التالي:

1. النسخة الأولى: **1.0.0**.
2. التحديث التصحيحي الأول: **1.0.1**.
3. التحديث التالي: **1.0.2**، ثم نستمر بنفس النمط.

حتى يتم تثبيت التحديث فوق النسخة السابقة دون إزالة التطبيق، يجب الحفاظ على:
- نفس `applicationId`.
- نفس مفتاح توقيع Android لكل إصدارات Release.
- زيادة `versionCode` في كل إصدار جديد.

لا يمكن ضمان التحديث فوق النسخة المثبتة إذا تغيّر مفتاح التوقيع أو معرّف التطبيق.

## 🛠️ إعداد البناء

يستخدم سير عمل GitHub Actions متغير المستودع التالي:

- `BACKEND_URL` — عنوان Cloudflare Worker المنشور.

إعداد الخدمة الخلفية يتطلب Cloudflare Worker وموارد الربط المحددة في `backend/wrangler.toml`. لا تضع مفاتيح API أو كلمات المرور أو ملفات التوقيع في المستودع.

## 🔐 الأمان قبل النشر

- يجب التحقق من مكافآت الإعلانات من جهة الخادم، وليس الوثوق بطلب التطبيق وحده.
- يجب تهيئة حدود الاستخدام والتحقق من مدخلات API.
- يجب إعداد توقيع Release بأسرار GitHub Actions.
- يجب مراجعة سياسة الخصوصية وتراخيص نماذج الذكاء الاصطناعي قبل النشر التجاري.

## 📋 سجل التغييرات

راجع [CHANGELOG.md](CHANGELOG.md).

## 📄 الترخيص

لم تتم إضافة ترخيص مفتوح المصدر بعد؛ جميع الحقوق محفوظة إلى حين تحديد الترخيص.

---

<p align="center">
  <strong>Mesut AI Studio</strong><br/>
  Made with passion · Powered by AI
</p>
