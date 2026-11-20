# Android build verification — October 22, 2026

Verified against Library source version 23, Scrappy-v4.9.6-camera-startup-guard-source.zip. Android version remains 4.9.6 (55). This checkpoint changes documentation only.

## Confirmed project contents

- Root settings.gradle.kts and build.gradle.kts, app/build.gradle.kts, Gradle 8.11.1 wrapper JAR and properties are present. The settings include both :app and :transport-tests.
- AndroidManifest.xml declares Camera and Internet permissions, the launcher activity, and CameraActivity. Cleartext HTTP is disabled. Debug builds use com.scrappy.app.preview and the label Scrappy Preview.
- CameraActivity includes permission recovery, asynchronous startup visibility/permission checks, local capture/review, retake and per-photo upload consent. Presence and source review are not device acceptance.
- AI_ENDPOINT and AI_PRIVACY_URL default to empty strings. No deployed Gemini service was verified. Release packaging requires configured HTTPS endpoint/privacy values. Gemini credentials belong on the server.
- .github/workflows/android-phone-build.yml already runs gateway checks, Android unit/transport tests, lint, debug packaging and APK inspection before uploading its preview artifact. This session did not execute that remote workflow or verify repository contents/access.

## Results from this session

- Node gateway tests: 27 passed using controlled provider responses, not live Gemini.
- Python APK-verifier tests: 7 passed using controlled SDK output, not a newly built APK. Run with `python3 -m unittest discover -s scripts -p 'test_verify_phone_apk.py'` from the project root.
- Release configuration wiring check: passed. This checks source wiring, not actual production URL values or deployment.
- Version consistency check: passed.
- `bash gradlew testDebugUnitTest lintDebug assembleDebug --console=plain`: failed downloading Gradle 8.11.1 with `java.net.SocketException: Network is unreachable`, before project evaluation or Android compilation. Java 17 is available; no Android SDK/build-tool executable was found in the checked standard locations/PATH. Kotlin and Android tests were not run in this session.
- No new APK/AAB, physical-phone test, live Gemini request or production release verification.

## Concrete next build gate

Run the existing Build Scrappy phone APK workflow from a repository containing this complete project, or run the Gradle command above in an Android SDK environment with dependency access. Require successful unit/transport tests, lint and assembleDebug, followed by scripts/verify_phone_apk.py on the resulting APK. Retain its package, version, minimum SDK and signing-certificate report with the APK.

Then install that exact APK on the phone and check camera preview, permission denial/Settings recovery, capture orientation, retake and background/restore during provider startup and photo review. Check AI separately with a configured test service and explicit photo consent. The current source checkpoint must not be presented as an already-built or phone-tested update.

Production blockers still include a verified Gemini deployment, published privacy policy, release signing and completed phone acceptance. See NEXT_ACCEPTANCE_MILESTONE.md for the broader functional and Play release gates.
