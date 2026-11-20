# Scrappy v4.9.6 — camera permission recovery Preview built

October 16, 2026 (UTC). Debug testing build, not a production Play release.

## Build and checks
Gradle 8.11.1, Java 17, Android SDK Platform 36 revision 2 and Build Tools 35.0.0 were used. The Gradle distribution checksum matched the project's pinned SHA-256. A working download/proxy route, full JDK and the environment's trusted certificate store resolved setup failures; TLS validation remained enabled. Platform-Tools were installed after the initial SDK availability warning.

`testDebugUnitTest lintDebug assembleDebug`: BUILD SUCCESSFUL. 154 Android unit tests + 15 JVM transport tests = 169 passed, no failures/errors/skips. Lint: zero errors/fatal issues, 79 warnings. Remaining lint warnings are recorded in the supplied report. Gateway source/test hashes match the earlier 25-test pass.

The APK includes the new onResume camera permission recovery and duplicate-startup guard. Its v2 debug signature is verified. Package com.scrappy.app.preview, versionName 4.9.6, versionCode 55, minSdk 26, targetSdk 36.

## Download identity
- APK: Scrappy-v4.9.6-permission-recovery-preview.apk
- Size: 7173434 bytes
- SHA-256: a15a0fe823ec080578fe9e7d7fa09056f9a072489865bec59349c607117776bb
- Source and evidence are supplied separately from the installable APK.

## Phone acceptance required
1. Install the new Preview APK and open Scrappy Preview. If updating fails due to a different debug signing key, back up saved data before uninstalling any old Preview.
2. Open Metal Photo, deny Camera, and verify the manual helper remains available.
3. In Android Settings → Apps → Scrappy Preview → Permissions → Camera, allow permission. Return through Recents. Expect a live preview and enabled Capture without reopening the screen.
4. Capture and review a photo. Repeat Settings round trips, including while reviewing; expect the same review photo and no duplicate camera startup failures.
5. Retake, rotate/background/restore, then complete NEXT_ACCEPTANCE_MILESTONE.md.

AI service/privacy URLs are empty; photo AI stays disabled/local in this Preview. Live Gemini, hosted privacy policy, physical-phone acceptance, production signing and Play submission remain pending. No release AAB was generated.

Evidence: verification/permission-build-2026-10-16/, including build log, all test XMLs, lint XML/HTML, APK signature/metadata and summary hashes.
