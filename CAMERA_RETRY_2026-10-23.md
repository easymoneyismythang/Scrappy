# Camera retry recovery — October 23, 2026 (New York)

Source checkpoint. Android version remains 4.9.6 (55).

Camera startup failure now exposes a Retry camera button on the same screen.
Retry requests permission if needed and restarts provider initialization; it
does not require closing and reopening the screen. Permission denial also
exposes retry and explains the Android Settings alternative. Android may stop
showing permission prompts after repeated denials; retry does not bypass that
restriction. The manual identification helper remains available during recovery.
Successful camera binding clears recovery controls, and photo review hides
the camera retry button. Capture stays disabled while startup is pending.

Added an Android instrumentation regression that invokes the shared startup
failure handler after a real camera preview becomes ready. It checks disabled
capture and visible retry/helper controls, retries the real camera, and then
requires a captured, decoded photo. This simulates failure handling; it does
not inject an actual provider exception. The Android suite now contains ten
instrumentation tests (seven camera flow, three photo processing).

Validation on this checkpoint:

- Gateway: 27 tests passed with controlled provider behavior.
- APK-verifier: seven Python tests passed with mocked SDK output.
- Release configuration and version consistency checks passed.
- Attempted testDebugUnitTest, lintDebug, assembleDebug and
  assembleDebugAndroidTest. The Gradle distribution download failed with
  Network is unreachable before compilation. See
  verification/camera-retry-2026-10-23/android-build.log.

The camera change and its Android regression have not been compiled or run.
No new APK/AAB or phone, emulator, live Gemini or Play acceptance is claimed.

GitHub inspection now confirms easymoneyismythang/Scrappy420 is accessible,
public, and has a README plus Scrappy-v4.9.5-Play-Store-ready-source.zip at its
root. It has no extracted Gradle project or workflow files at the root.
Uploading a ZIP alone does not activate its enclosed build workflows. The
latest source was not uploaded to that public repository because the user
previously chose private visibility. Repository visibility was not changed.

Next build gate: place the extracted latest source into the intended private
repository, then run the included phone-build and emulator workflows. Require
all ten instrumentation tests to pass before a physical phone test of startup
retry, permission denial/Settings recovery, retake and photo orientation.
Production Gemini configuration/privacy, signing and Play acceptance remain
separate unfinished release gates.
