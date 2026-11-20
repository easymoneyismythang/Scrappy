# Scrappy 4.9.6 — Android build completed

October 12, 2026. Preview package `com.scrappy.app.preview`, versionCode 55, minSdk 26 / targetSdk 36.

## Build and verification
- `testDebugUnitTest lintDebug assembleDebug`: BUILD SUCCESSFUL.
- 141 Android unit tests passed, zero failures/errors/skipped.
- Lint: zero errors, 80 warnings. Remaining warnings cover localization, dependency versions, API/style suggestions, orientation, WebView JavaScript and launcher icon readiness.
- APK signature verified using v2; this is a debug-signed testing APK, not a signed Play production release.
- Release configuration and document consistency checks passed.
- APK: `Scrappy-v4.9.6-phone-preview.apk` (7173434 bytes).
- SHA-256: `8f59cfbdf60a722fdd384c7e54a92dfd48122a00b4b1316621c5e54a72391661`.
- Full current build log, test XMLs, lint reports and APK metadata are in verification/android-build-2026-10-12/.

## Source fixes needed to compile
- Imported java.net.URI in the Gradle script to avoid a name conflict with Gradle's Java configuration.
- Replaced the nonexistent arrayOfNotNull call in camera cleanup with listOfNotNull(...).toTypedArray(), preserving release of only this screen's use cases.
- Preserved all previous camera, weather, gateway deadline and bounded-response fixes.

## Install and test
Download the APK onto your Android phone and open it. Android may ask you to allow installation from the app opening the file. The launcher name is Scrappy Preview. It uses separate storage from the production package; back up existing records before transferring anything.

The preview has no AI endpoint/privacy URL configured, so Gemini photo identification is disabled. It includes local camera/manual helpers and the other implemented workflows. Prices and yard contacts remain manually entered or clearly labeled examples. Physical phone camera/device acceptance and live-provider validation have not been performed. Follow PRELAUNCH_TEST_MATRIX.md after installation.

This completes the preview Android build. Production gateway hosting/configuration, public privacy policy, production signing, remaining store assets and Play acceptance are still pending. No production release AAB was built.
