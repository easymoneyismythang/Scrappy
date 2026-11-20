> Later verification: the Android build and all 169 Gradle tests now pass, with a separately supplied Preview APK. See ANDROID_PERMISSION_BUILD_2026-10-16.md. Build-blocked notes below describe the earlier source-only checkpoint; physical-phone acceptance remains pending.

# Scrappy v4.9.6 — camera permission recovery

October 16, 2026. Version remains 4.9.6 (55). Source checkpoint, not a new installable build.

## Problem and implementation
After denying Camera, allowing it from Android Settings does not invoke the original permission request callback. The screen had no resume recovery path, so returning could leave Capture disabled. CameraActivity now checks permission on resume and starts the camera when there is no review photo. Startup is guarded while pending and while a capture use case is already present, preventing duplicate binds from onResume and the permission callback. If permission is missing while a use case remains, it is released and the manual helper is offered. Returning while reviewing a captured photo preserves that review.

## Test reliability correction
The first gateway run passed 24/25 tests: the trickling-upload client saw ECONNRESET as the gateway closed the timed-out connection while the client was still writing. The raw socket helper now accepts that reset only after receiving the final chunk terminator of a complete HTTP response. A reset before a complete reply still fails; status, budget and retry assertions remain. No gateway production code changed. The subsequent full suite passed 25/25; five additional focused runs passed.

## Verification and limits
Node syntax and both release scripts pass. Android build could not start: Gradle download failed with Network is unreachable. No Kotlin suite, Android build, lint, emulator or device test was run for this change. Historical build results do not cover the new camera source. Evidence is in verification/permission-recovery-2026-10-16/.

## Required next verification
1. On a machine with Java 17, Android SDK 36 and Gradle dependencies available, run ./gradlew testDebugUnitTest lintDebug assembleDebug.
2. Install the freshly built Preview APK. Deny camera permission; verify manual helper remains available.
3. Allow Camera in Android Settings, return through Recents; expect a live preview and enabled Capture without reopening the screen. Capture and review a photo.
4. Repeat Settings round trips with permission granted; verify no duplicate bind/start failures. Repeat while reviewing a photo; expect the same review, no restarted preview.
5. Deny again from Settings (Android may restart the app), reopen camera, verify denial/manual fallback; allow and repeat recovery.
6. Run the full NEXT_ACCEPTANCE_MILESTONE.md phone checklist. Live HTTPS gateway/Gemini, privacy policy, production signing and Play acceptance remain pending.
