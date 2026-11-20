# Camera review fix — October 10, 2026

Continued from Scrappy-v4.9.6-camera-weather-fixes.zip.

The camera previously stayed bound while its preview was hidden behind the captured-photo review. Review now releases the camera, and Retake reacquires it through the existing permission check. A late camera-start callback also skips binding if a captured photo is already being reviewed. Activity destruction explicitly releases its camera binding.

Verification in this session:
- AI gateway: 12 tests passed, zero failed.
- Release configuration check: passed.
- Release/document consistency check: passed for 4.9.6 / 55.
- Android build attempted: blocked while downloading Gradle with `Network is unreachable`, before compilation. Android compilation, lint, and device behavior remain unverified.

Next device check: capture a photo, confirm the camera privacy indicator turns off during review, tap Retake and confirm the preview resumes, then repeat after rotation and leaving/reopening the screen. Verify front-camera fallback and camera permission denial as well.

This is an updated source package, not an APK. The existing GitLab pipeline is the available route to compiling a fresh phone preview. Production AI configuration, phone acceptance, signing, and Play submission remain pending.
