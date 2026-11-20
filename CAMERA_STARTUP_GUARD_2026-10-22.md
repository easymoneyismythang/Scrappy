# Camera startup visibility and permission guard

October 22, 2026 (America/New_York). Source checkpoint; version remains 4.9.6 (55).

The latest supplied phone image, 10705.jpg, shows Scrappy v4.9.6's home screen running. It confirms a home-screen launch on that device, but does not identify the installed APK/signing key or confirm camera capture or Gemini analysis. The earlier installation failure must not be assumed to affect every build.

CameraActivity now rechecks both RESUMED lifecycle state and camera permission when the asynchronous camera-provider initialization completes, before binding camera use cases. If the user has left the screen or revoked permission while initialization was pending, startup exits with Capture disabled. The existing onResume path retries when the screen is visible and permission is granted. Photo review remains protected by the existing lastPhoto check.

Validation in this turn:
- 27 Node gateway tests passed using controlled provider behavior.
- Seven Python APK verifier tests passed using controlled SDK output.
- Release configuration and version consistency scripts passed.
- Android testDebugUnitTest, lintDebug and assembleDebug were attempted together. Gradle failed to download 8.11.1 with Network is unreachable before Android compilation. Android SDK is unavailable here. The activity change is source-reviewed only; the passing gateway/verifier checks do not exercise it.
- No new APK was produced. No live Gemini request or physical-phone camera test was performed.

Next verification in an Android build environment:
1. Compile and run testDebugUnitTest, lintDebug and assembleDebug; verify the resulting APK using scripts/verify_phone_apk.py.
2. Open the camera, immediately leave it while provider startup is pending, then return. Expect Capture disabled while away and a working preview after return, with no startup error or duplicate binding.
3. Revoke camera permission during startup and return. Expect no capture access; allow permission in Settings and return, expecting recovery.
4. Capture a photo, inspect orientation, retake, and repeat the permission/recreation acceptance checks in NEXT_ACCEPTANCE_MILESTONE.md.
5. Verify configured Gemini analysis separately; home-screen launch is not evidence that the service is configured.
