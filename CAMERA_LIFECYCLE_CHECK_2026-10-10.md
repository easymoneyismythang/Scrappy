# Camera lifecycle follow-up — October 10, 2026

Continued from version 2 of Scrappy-v4.9.6-camera-weather-fixes.zip.

Changes:
- Camera cleanup unbinds only the preview and capture use cases owned by this activity. ProcessCameraProvider is shared, so an old activity's cleanup must not unbind another activity's camera.
- Startup failures release the activity's use cases, including those assigned before a failed bind.
- Capture error callbacks ignore a finishing activity and enable Capture only if its capture use case still exists.
- The AI consent callback checks that the activity is active, the selected photo is still current, and no analysis is already running before scheduling an upload.

Verification in this session:
- AI gateway: 12 tests passed, zero failed (unchanged gateway).
- Release configuration gate: passed.
- Release/document consistency: passed for Android 4.9.6 / 55.
- Android tests, lint and APK build attempted: blocked downloading Gradle 8.11.1 with `Network is unreachable`, before compilation. These checks have not passed for this source revision.

Phone acceptance still required: capture/review/retake; rotate during preview and review; leave and reopen the camera rapidly; deny permission; cancel AI consent; rotate with consent open; retry after capture failure. Check that review releases the camera privacy indicator and Retake restores the preview.

This package is source, not a new APK. The GitLab build pipeline remains the route to obtaining a freshly compiled phone test APK. Production AI deployment/configuration, phone acceptance, release signing and Play submission remain pending. Earlier verification files describe earlier revisions; this note records the checks actually performed on this revision.
