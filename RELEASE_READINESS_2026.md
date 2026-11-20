> Current v4.9.0: 136 core/Android tests, 12 gateway tests, zero-error lint, Preview APK and unsigned release APK/AAB passed. AI disabled in supplied Preview. Phone/provider/Play acceptance pending. Older notes below are historical. See IMPLEMENTATION_STATUS.md.

> v4.8.0 adds saved-quote yard comparison previews. See IMPLEMENTATION_STATUS.md for current verification. No production AI/live price integration or Play release is claimed.

> Current status: v4.7.0 / versionCode 48. 108 tests, debug APK, zero-error lint and unsigned minified release build passed. Phone testing, signing and Play submission remain pending. See IMPLEMENTATION_STATUS.md. Statements below are historical planning, not current policy guidance.

> v4.4.0 status: development source, not a verified Play release. See README.md and IMPLEMENTATION_STATUS.md for current capabilities and blockers. Weather/radar introduce external data requests; prior offline-only disclosures are superseded by PRIVACY_POLICY_DRAFT.md.

# Scrappy v4.2.0 — Release Readiness

## Completed in this milestone
- targetSdk / compileSdk remain API 36 for current Google Play submission requirements.
- versionCode 45 / versionName 4.4.0.
- Camera capture now has an explicit review and retake state.
- Temporary camera files are deleted when retaken or when the camera activity closes.
- Camera UI explicitly states that this beta does not upload or claim AI identification.
- Camera failure and denied-permission states remain usable rather than silently failing.
- Manual Identification Helper is reachable directly from photo review.

## Play Console gates still requiring the developer account / production services
- Reserve/register the final package name in Play Console and complete developer identity verification as applicable.
- Generate and protect the upload signing key, enroll in Play App Signing, and build the signed release AAB.
- Run the AAB through internal/closed testing and Play pre-launch reporting.
- Supply final app icon, feature graphic, phone screenshots, support contact, and a publicly hosted privacy-policy URL.
- Complete Data safety, content rating, ads declaration, target audience, app access, and other Play Console forms based on the final production behavior.
- If Gemini photo analysis is enabled, deploy a secure backend/proxy, finalize disclosure/consent/retention behavior, and update the privacy/Data safety answers before release.
- Replace demo scrapyard and benchmark-price data with an approved live source, or clearly retain them as non-live reference/demo data.

## Release rule
Do not represent Scrappy as providing live AI identification, live yard pricing, or live nearby-yard discovery until those production services are actually connected and tested.
