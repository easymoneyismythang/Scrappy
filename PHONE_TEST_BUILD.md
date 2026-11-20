# Scrappy 4.9.6 phone preview

Download Scrappy-v4.9.6-updated-preview.apk and open it on Android. If prompted, allow installation from the app opening the file. Launch Scrappy Preview.

Test Camera permission, capture and local photo review, Retake, and the manual identification helper. Then run PRELAUNCH_TEST_MATRIX.md. The APK includes the latest client deadline fix and passed 145 Android unit tests and lint with no errors.

Gemini uploads are disabled in this preview because the AI endpoint/privacy URL are not configured. It is a debug-signed phone-test build; production release and physical-phone acceptance are pending.

If Android reports a package/signature conflict, keep your existing app and export its ledger backup before reinstalling. Uninstalling erases that app's local records. Preview and production packages use separate storage.

See ANDROID_PREVIEW_REBUILD_2026-10-12.md for verification evidence. A full Java 17 JDK, SDK 36, build-tools 35.0.0 and Gradle 8.11.1 are required for rebuilds.
