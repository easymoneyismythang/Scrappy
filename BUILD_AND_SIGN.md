# Build and sign Scrappy v4.9.6

Current Android metadata: package `com.scrappy.app`; versionCode `55`; versionName `4.9.6`; minSdk 26; compile/target SDK 36. Use a full Java 17 JDK, the included Gradle 8.11.1 wrapper, and Android SDK 36.

## Verified in the current release-candidate source

- `python3 scripts/check_release_config.py` passes.
- `python3 scripts/check_release_consistency.py` passes.
- `cd backend && npm test` passes 12/12 tests.
- A fresh Android/Gradle compile cannot be honestly claimed from the current ChatGPT workspace because the Gradle 8.11.1 distribution is not cached and outbound access to `services.gradle.org` is unavailable. Historical Android test/lint/build evidence remains under `verification/`, but it is not a substitute for a fresh v4.9.6 build.

## Fresh Android verification required before Play upload

Open this project in Android Studio with internet access and let Gradle sync. Configure the production properties in a non-committed Gradle properties location:

- `scrappyAiEndpoint=https://YOUR_HOST/v1/identify`
- `scrappyAiPrivacyUrl=https://YOUR_PUBLIC_PRIVACY_POLICY`

Then run:

`./gradlew clean testDebugUnitTest lintDebug assembleDebug bundleRelease`

The release packaging task intentionally fails if the AI endpoint or privacy URL is missing/unsafe. Do not bypass this gate.

Install the fresh preview/debug APK on a physical Android phone and complete `PRELAUNCH_TEST_MATRIX.md` / `NEXT_ACCEPTANCE_MILESTONE.md`. Do not use the historical v4.9.0 artifact in `build-artifacts/` as proof of v4.9.6 acceptance.

## Signing

For Google Play, use Android Studio → Build → Generate Signed App Bundle or APK → Android App Bundle, or configure a secure external signing setup. Keep the upload keystore and passwords outside this project. Expected bundle output is `app/build/outputs/bundle/release/app-release.aab`.

Never commit upload signing keys/passwords, Gemini keys, backend secrets, `keystore.properties`, or `scrappy-release.properties`. Complete `PLAY_STORE_RELEASE_CHECKLIST.md` before publication.

Current version: 4.9.7 — dark ScrapRight-style UI redesign (Prices / Scan / Yards / More tabs, real Covington yards, Georgia index prices).
versionCode `56`
