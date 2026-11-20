# Scrappy v4.9.6 — Google Play release checklist

Current Android package: `com.scrappy.app`  
Version: `4.9.6` (`versionCode 55`)  
Minimum Android: API 26  
Compile/target SDK: API 36 (Android 16)

## Verified in this source package
- [x] `compileSdk = 36` and `targetSdk = 36`.
- [x] Release packaging is minified and resource-shrunk.
- [x] Release builds fail when the production AI endpoint or public privacy-policy URL is missing/unsafe.
- [x] Camera permission is declared; camera hardware is optional.
- [x] Cleartext HTTP is disabled and Android backup is disabled.
- [x] No Gemini API key is embedded in the Android app.
- [x] AI uploads require an explicit per-photo consent action in the app.
- [x] Backend automated suite passes 32/32 in the November 3, 2026 checkpoint (controlled test provider; not live Gemini).
- [x] Stale v4.9.0 unsigned AAB removed from the Play handoff package.

## Required before upload
- [ ] Publish the final privacy policy at the HTTPS URL used as `scrappyAiPrivacyUrl`.
- [ ] Deploy the production Scrappy AI gateway at an HTTPS URL ending `/v1/identify`; keep the Gemini key server-side only.
- [ ] Review Google/Gemini, Open-Meteo, and NOAA/NWS provider terms and production logging/retention.
- [ ] Build with the production endpoint and privacy URL Gradle properties.
- [ ] Run a fresh clean Android build, unit tests, lint, and `bundleRelease` with network/dependency access.
- [ ] Install/test the corresponding build on a physical Android phone: launch, camera permission deny/allow, capture/retake, AI consent/send/error recovery, weather, radar, yards/call/directions, quotes, calculators, ledger, import/export/share, process death/relaunch.
- [ ] Inspect the release AAB/APK for package/version/permissions and verify no secret is packaged.
- [ ] Sign the AAB with the developer's protected upload key and enroll/use Play App Signing.
- [ ] Upload to Play Console testing track and resolve automated pre-launch report findings.
- [ ] Complete App content: privacy policy, Data safety, ads declaration, target audience, content rating, app access, and any other forms Play Console presents.
- [ ] Finalize app icon, feature graphic, phone screenshots, support email/site, category, and store listing.
- [ ] Complete account-specific testing requirements before Production access.

## Current Google Play timing note (October 2026)
New Android phone apps and updates must target Android 16 / API 36. Scrappy does. Confirm Play policy again on submission day because requirements can change.

No signed AAB, physical-device acceptance result, Play Console form submission, or Google approval is claimed by this source package.
