# Separate camera test build — October 27, 2026 (New York)

A debug-signed cameraTest variant now installs as Scrappy Camera Test, package
com.scrappy.app.cameratest. It can be installed beside com.scrappy.app.preview
or com.scrappy.app without needing either app's signing key. Separate Android
package storage means existing loads, quotes and sales are not automatically
copied or modified. This is a test companion, not a production identity change.

The new variant inherits debug behavior and shares the existing camera/photo
implementation. Its AI endpoint and privacy URL are empty, confirmed in the
built BuildConfig. It does not enable live Gemini. Minimum API 26, target API
36, version 4.9.6 (55). No signing keys are included in this source package.

Gradle testCameraTestUnitTest, lintCameraTest and assembleCameraTest succeeded.
All 154 variant unit tests passed. The unchanged 19 transport tests were
up-to-date from the previous successful run, not an additional new execution.
Lint had zero errors/fatal issues and 74 warnings. Real SDK tools verified the
APK's signature, package, label and version. All 15 APK-verifier tests passed.
The verifier defaults to Preview and requires --variant camera-test for this
companion; wrong Preview/production identities are rejected. Release checks
passed. The actual Gradle release validation task was also executed and correctly
rejected the missing production AI endpoint. CI now builds, verifies and uploads both test APK identities; its YAML
parsed locally, but that remote workflow was not run in this session.

A software-emulated Android API 35 device was attempted because KVM is not
available here. The software emulator did not complete boot within the 600-second
deadline. No instrumentation tests executed; compiled camera/photo tests must
not be counted as passed. The task emulator was stopped after the attempt. The execution
workspace briefly disconnected during this attempt and recovered with files
intact. See verification/camera-test-2026-10-27 for recorded evidence.

Phone installation and capture/retake/permission/lifecycle acceptance remain
pending. See CAMERA_TEST_INSTALL.md for the non-destructive phone test steps.
Live Gemini setup/acceptance, verified live data services, privacy/store assets,
production signing and Play testing/submission remain separate release gates.
