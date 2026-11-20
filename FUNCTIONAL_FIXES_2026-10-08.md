# Scrappy functional fixes — October 8, 2026

Continued from the SDK-fixed v4.9.6 phone-build package.

- Camera permission denial and camera startup failure now expose the manual identification helper.
- Camera capture remains disabled until binding succeeds. Retake checks permission again, including after a restored photo review.
- Devices with only a front camera can use it when a back camera is unavailable.
- Retake is disabled during AI processing to avoid deleting a photo while the worker reads it.
- Repeated refresh taps on the same weather screen no longer queue duplicate weather requests.
- Main-frame radar network and HTTP errors remain visible after WebView's page-finished callback. Retry resets the error state.
- Home and privacy screens use the build's version instead of stale hardcoded text.

Verification: release configuration and release/document consistency checks pass. Previous backend verification remains 12/12 passing; no backend code changed. Android compilation and physical-device behavior remain unverified because this environment lacks the Android SDK and cannot download Gradle dependencies. No new APK is included.

Device checks for the next successful APK: deny camera permission and open the helper; capture and retake; recreate the activity from photo review; attempt AI then retake; repeatedly refresh weather; open radar offline, retry online, and confirm error/success messages.
