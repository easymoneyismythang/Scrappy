# Scrappy v4.9.6 — cancel photo analysis

The photo review screen now exposes Cancel analysis while an approved
analysis request is running. Cancel disconnects its transport, invalidates
the pending result callback, preserves the local photo, and re-enables
analysis and retake. Preparing a canceled photo cannot subsequently upload it:
the existing cancellation check runs again after JPEG preparation. Photos
already received by the service cannot be retracted by client cancellation.

Normal completion hides Cancel analysis. Retake also defensively cancels any
attached request. A new camera instrumentation test uses a captured local
photo and a controlled attached request, verifies cancel closes the transport
while preserving the photo, then retakes and captures again. It sends no photo
to Gemini. This test simulates an in-flight request; it does not exercise the
consent dialog or a live provider round trip.

Android packaging was attempted with `assembleCameraTest` and failed while
downloading Gradle 8.11.1 with `Network is unreachable`. No Android SDK is
configured here. The new Android code and device test are not compiled or
executed, and no new APK was produced. Existing host test results do not
verify this Android UI change. Fresh CI compilation and device/emulator
acceptance remain required. No live Gemini/phone acceptance is claimed.

Phone acceptance after a fresh configured build:

1. Capture a photo, consent to analysis, then tap Cancel analysis while busy.
2. Confirm the photo stays visible, retry/retake become available, and no
   delayed result replaces the cancellation message.
3. Retry with fresh consent and confirm the new result is displayed.
4. Cancel another analysis, retake, and capture another photo. Also test Back
   and rotation while busy; service processing may already have started.

GitHub inspection in this session found `easymoneyismythang/Scrappy420`
accessible and public, with only README.md and the v4.9.5 source ZIP at the
repository root. No extracted Android project or workflow is present there.
The separate Scrappy repository reports size zero. No remote source or
visibility was changed. A repository containing the extracted current source
and build workflow is still needed for the remote APK build route.
