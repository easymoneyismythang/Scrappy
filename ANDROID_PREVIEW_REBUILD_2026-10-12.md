# Scrappy 4.9.6 — verified preview rebuild

October 12, 2026. Latest preview APK: Scrappy-v4.9.6-updated-preview.apk.

## Completed verification
- Gradle 8.11.1: testDebugUnitTest lintDebug assembleDebug — BUILD SUCCESSFUL.
- Android unit tests: 145 passed; no failures, errors or skipped tests.
- Lint: zero errors/fatal issues and 78 warnings. Remaining warnings are retained in the included report; they still require review before Play release.
- Node gateway tests: 21 passed; provider mocked.
- APK Signature Scheme v2 verified. Debug signed testing package, com.scrappy.app.preview, versionName 4.9.6 / versionCode 55, minSdk 26 / targetSdk 36.
- SHA-256: a960a4a5eb0bdd65445ab7b46b31d20ef02b14d4e739d66f71a0a232c2033bcf.
- Size: 7173434 bytes.
- Full build log, test XMLs, lint reports and APK metadata: verification/android-preview-rebuild-2026-10-12/.

The build environment was repaired with a complete Java 17 JDK and its configured dependency proxy. No project repositories were changed to mirrors and no release gate was bypassed.

## Included work
The rebuilt APK includes the client response deadline from CLIENT_RESPONSE_DEADLINE_2026-10-12.md and previous camera recovery fixes. The updated source also contains the independently tested server disconnect cancellation fix in GATEWAY_DISCONNECT_FIX_2026-10-12.md; that fix requires backend deployment.

## Phone acceptance and remaining release work
Download the APK and open it on Android. Launch Scrappy Preview, grant Camera permission, capture a photo, inspect local review, Retake, then open the manual identification helper. Repeat with Camera permission denied, background/resume and rotation. Complete PRELAUNCH_TEST_MATRIX.md and report any unexpected behavior.

This preview still has empty AI_ENDPOINT and AI_PRIVACY_URL, so Gemini uploads are disabled. Physical-phone acceptance and real Gemini calls were not performed. Production gateway/privacy configuration, production signing, remaining store assets and Play acceptance remain pending. No production AAB was built.

This is a newly debug-signed preview. If Android reports a conflicting package/signature, keep the installed app and export its ledger backup before any reinstall. Uninstalling removes that app's local records. Production and preview storage are separate.
