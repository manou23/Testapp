# Mesut AI Studio — Test checklist

Use this checklist before publishing a release. A successful Gradle build confirms compilation only; it does not prove that the AI backend, quotas, privacy controls or all device-specific flows work.

## 1. Build and install

- [ ] GitHub Actions workflow completes successfully.
- [ ] Downloaded APK installs on Android 8.0 (API 26) or later.
- [ ] App launches without crashing.
- [ ] Package name remains `com.mesut23.aistudio`.
- [ ] App version shown in About is correct.

## 2. Interface and localization

- [ ] Home, Create, Edit, Creations and Settings tabs open.
- [ ] Arabic text is readable and laid out correctly.
- [ ] French and English translations are complete.
- [ ] Changing language updates the app labels.
- [ ] Navigation back behavior is correct.

## 3. Image workflows

- [ ] Generate rejects an empty prompt.
- [ ] Generate returns a valid image when the backend is configured.
- [ ] Each aspect ratio produces the expected crop.
- [ ] Image picker opens and accepts a supported photo.
- [ ] Edit sends the selected image and instruction to the backend.
- [ ] Network and quota errors are displayed without crashing.
- [ ] Generated images appear in Creations.
- [ ] Save to gallery works.
- [ ] Share opens Android's share sheet.
- [ ] Favorite and delete actions persist correctly.
- [ ] Delete all removes the saved creations.

## 4. Backend and security

- [ ] Worker is deployed with the AI binding.
- [ ] KV namespace is bound as `LIMITS`.
- [ ] GitHub Actions variable `BACKEND_URL` points to the deployed Worker.
- [ ] Requests without a valid device identifier are rejected.
- [ ] Daily usage limit is enforced.
- [ ] Invalid or oversized payloads are rejected.
- [ ] No provider secret is present in the APK or repository.
- [ ] `/reward` remains disabled until AdMob SSV signature verification is implemented.

## 5. Release checks

- [ ] Privacy policy is reviewed and published at a stable URL.
- [ ] Image processing and retention disclosures match actual backend behavior.
- [ ] AdMob production IDs replace test IDs only after account approval.
- [ ] Production signing key is backed up securely.
- [ ] Release APK is signed with the same key as the previous release.
- [ ] `applicationId` remains unchanged and `versionCode` is incremented.
- [ ] Upgrade is tested over the previous installed version.
- [ ] Release notes and APK checksum are published.

## Known limitations in 1.0.0

- The APK workflow currently creates a **debug** build artifact.
- Production signing and public Releases upload are not configured.
- Backend deployment and Cloudflare KV namespace setup are operator tasks.
- Rewarded credits are deliberately disabled pending server-side verification.
- Premium billing is not implemented.
