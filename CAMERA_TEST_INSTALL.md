# Install the separate Scrappy Camera Test app

This test app can install beside Scrappy and Scrappy Preview. You do not need
to remove either app. It has separate local storage and does not automatically
copy your saved loads, quotes or sales. Keep existing data in your current app.

1. Download `Scrappy-v4.9.6-camera-test.apk` on your Android phone.
2. Tap the downloaded APK. If Android asks, allow installation from the app you
   used to download it, then finish installation.
3. Open **Scrappy Camera Test** (the new app name).
4. Tap **Camera Metal Identifier**, allow camera access, and capture one item in good light.
5. Confirm the captured photo appears. Tap **Retake** and capture another photo.
6. Press Back, reopen Camera Metal Identifier, and repeat. Also check returning after
   backgrounding the app and denying/re-enabling camera permission.

AI analysis is not configured in this build. An “AI service is not configured”
message is expected; use the identification helper for now. This test does not
establish live Gemini operation. Photos are not uploaded without configuration
and consent. Report the exact Android/app message if a step fails.

## Developer details

| App label | Package | Role |
| --- | --- | --- |
| Scrappy | com.scrappy.app | Production identity; no production release supplied |
| Scrappy Preview | com.scrappy.app.preview | Existing ordinary Preview |
| Scrappy Camera Test | com.scrappy.app.cameratest | Separate local camera-test installation |

The cameraTest build type inherits debug behavior and shares the same camera,
photo encoder and manual-helper source. It is version 4.9.6 (55), minimum API
26, target API 36. It is debug-signed. Empty AI and privacy configuration was
verified in the built BuildConfig. No signing keys are distributed.

Build with Java 17, Android SDK 36/build tools 35.0.0:

```bash
./gradlew testCameraTestUnitTest lintCameraTest assembleCameraTest
python3 scripts/verify_phone_apk.py app/build/outputs/apk/cameraTest/app-cameraTest.apk --variant camera-test
```

Supply `--aapt` and `--apksigner` paths if SDK tools are not on PATH. The verifier
requires an explicit camera-test variant; the ordinary Preview remains the
default expected identity. The phone-build CI workflow now builds, verifies and
uploads both test app identities. Runtime phone acceptance remains separate from
build/signature checks. This companion app is for testing, not a Play release.
