# Camera provider startup recovery — October 23, 2026 (New York)

Source checkpoint; Android version remains 4.9.6 (55).

CameraActivity now handles exceptions thrown directly by ProcessCameraProvider.getInstance, before the asynchronous listener can run. The screen disables capture and offers the manual identification helper through the same recovery path as asynchronous initialization failures. Previously, this synchronous failure escaped the startup handler. This is a source-reviewed fix; it has not been compiled or exercised on Android.

Validation: 27 gateway tests passed with controlled provider behavior. Seven Python APK-verifier tests passed with mocked SDK output. Release configuration wiring and version consistency checks passed. These checks do not exercise CameraActivity.

Android testDebugUnitTest, lintDebug and assembleDebug were attempted; the Gradle distribution download failed with Network is unreachable before compilation. No APK/AAB was generated. Connected GitHub access to easymoneyismythang/Scrappy- returned 404; this does not establish whether the repository exists or is private.

Next gate: compile the latest source in an Android environment, run unit/transport tests and lint, verify the resulting Preview APK, then test startup, permission recovery and photo capture on the phone. Inject a synchronous provider initialization failure in an Android test harness and verify the screen remains open with Capture disabled and the manual helper usable. Reopen after the failure clears and verify normal preview recovery. Live Gemini, production configuration and release signing remain unverified.
