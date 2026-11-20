# Android camera automation — October 23, 2026 (New York)

Source checkpoint only. Android version remains 4.9.6 (55).

## Added

Six Android camera-flow instrumentation tests cover capture/retake and a second capture, captured-photo restoration after recreation, preview recovery after backgrounding, immediate background/return during startup, preview recreation, and retention of local photo review after backgrounding. They grant camera permission on the test device, wait for enabled capture controls and decoded image review, and never choose AI upload. Immediate backgrounding exercises the transition but cannot guarantee provider initialization is still pending on every run.

Three Android photo-processing instrumentation tests exercise all eight EXIF orientations against independently specified colored quadrants, check location/user-comment metadata removal during upload reencoding, and verify a 3200x2400 photo decodes to a bounded 1600x1200 review. They use generated local images and no external provider.

The ordinary phone-build workflow now also compiles the instrumentation APK with assembleDebugAndroidTest. A separate manually triggered camera workflow runs connectedDebugAndroidTest on an API 35 x86_64 Pixel 7 Pro emulator with emulated cameras and retains test reports. AndroidX runner/core/ext-junit test dependencies are declared in app/build.gradle.kts. No additional production dependency was introduced.

## Validation and limits

Both workflow YAML files parsed and their shell run blocks passed bash syntax checks. Seven existing Python APK-verifier tests passed using mocked Android SDK output. Release configuration wiring and version consistency checks passed. The unchanged gateway previously passed 27 controlled-provider tests in the prior checkpoint; it was not rerun for test/workflow-only changes.

Attempted testDebugUnitTest, lintDebug, assembleDebug and assembleDebugAndroidTest. Gradle download failed with Network is unreachable before project evaluation or compilation; log: verification/camera-provider-2026-10-23/android-instrumentation-build.log. No Android SDK, emulator, adb, Android platform JAR or Kotlin compiler was found in checked local paths. Therefore the nine new tests are written and source-reviewed, but neither compiled nor run. YAML/shell checks do not validate GitHub execution or Android behavior.

A connected GitHub installed-repository search returned no Scrappy repositories. The private repository cannot be inspected, updated or built through that connection at present. No remote workflow was executed. This source ZIP must be extracted into a complete repository tree; uploading the ZIP alone does not activate the workflows inside it.

## Run the next gate

In an Android SDK environment with dependency access:

    sh gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
    sh gradlew connectedDebugAndroidTest

Alternatively, after this complete project is available to the GitHub connection, run Build Scrappy phone APK and Test Scrappy camera on Android emulator. Require nine instrumentation tests passing, then verify/install the resulting Preview APK and perform physical-phone permission denial/Settings recovery, orientation and real-camera acceptance. The emulator tests do not cover permission revocation, synchronous provider-failure injection, process death, live Gemini, or production release acceptance.

Completion remains blocked by build access, device verification, a configured and verified Gemini deployment/privacy URL, release signing and Play release acceptance. No new installable APK/AAB was produced and no completion percentage is inferred.

Implementation references checked: https://developer.android.com/reference/androidx/test/core/app/ActivityScenario and https://github.com/ReactiveCircus/android-emulator-runner (October 23, 2026).
