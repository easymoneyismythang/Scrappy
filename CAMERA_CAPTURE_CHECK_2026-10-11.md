# Camera capture follow-up — October 11, 2026

Continued from version 3 of Scrappy-v4.9.6-camera-weather-fixes.zip.

Changes:
- Capture now reads the current display rotation immediately before taking the photo. This covers a 180-degree phone turn that does not recreate the activity.
- Synchronous capture-start errors delete the temporary file and restore the capture button when a capture use case remains available, with a retry/manual-helper message.
- Only one AI consent dialog can be open. Retake and activity destruction dismiss it, and dismissal clears the reference.

Checks actually performed on this revision:
- AI gateway: 12 tests passed, zero failed; gateway unchanged.
- Release configuration gate: passed.
- Release/document consistency: passed for Android 4.9.6 / 55.
- Android unit tests, lint and debug APK build attempted. Gradle 8.11.1 download failed with `Network is unreachable` before compilation. No Android compilation or new APK is verified for this revision.
- Source review of capture error recovery and consent dialog lifecycle completed. Device behavior remains unverified.

Phone acceptance still required:
1. Capture at normal orientation and after turning the phone 180 degrees; check review and uploaded image orientation.
2. Tap Analyze repeatedly; verify only one consent prompt opens. Cancel, reopen, and send once.
3. Rotate or leave while consent is open; verify the old prompt closes and the photo is not sent from that prompt.
4. Retry after capture failure; check the button works and no stale photo appears.
5. Repeat capture/review/retake; check the camera privacy indicator stops during review and resumes for Retake.

This package remains source, not an installable APK. Use the included GitLab pipeline to compile a fresh phone test APK. Production AI service configuration, phone acceptance, release signing and Play submission remain pending. Older verification reports apply to their recorded revisions only.
