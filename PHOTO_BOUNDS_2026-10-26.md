# Photo decoding checkpoint — October 26, 2026 (New York)

Android version remains 4.9.6 (55). This is a source checkpoint.

The photo decoder now uses ceiling division when choosing its power-of-two
sample size. Previously, an odd dimension such as 3201 divided by two rounded
down to 1600 in the size check, although the image decoder can round the
result up. The revised check keeps the sampled dimensions within the intended
1600-pixel bound before orientation correction and JPEG upload.

Added Android regression cases for odd landscape and portrait dimensions,
including a dimension just above the limit. Added an unreadable-photo test
requiring rejection before an upload image can be produced. There are now
twelve Android instrumentation tests: seven camera flow and five photo tests.

Verification performed in this session:

- All 173 standalone Kotlin/JVM tests passed.
- All 27 gateway tests passed with controlled provider behavior.
- All seven APK-verifier unit tests passed with mocked SDK output.
- Release configuration and version consistency checks passed.
- The wrapper's direct Gradle download failed with Network is unreachable.
- A Python transfer successfully downloaded Gradle 8.11.1 and verified its
  published SHA-256 checksum. Android SDK 36, build tools 35.0.0 and a complete
  JDK 17 were installed. Java used the available proxy and system certificate
  trust store for dependency downloads.
- testDebugUnitTest, lintDebug, assembleDebug and assembleDebugAndroidTest
  completed successfully: 154 Android unit tests plus 19 JVM transport tests,
  zero failures; lint had zero errors/fatal issues and 73 warnings.
- A fresh Preview APK was produced and its signature, package and version
  verified by the real SDK tools. The camera instrumentation APK compiled;
  its DEX contains the retry and new photo regression tests.

See verification/photo-bounds-2026-10-26 for test and build logs. The JVM and
gateway tests do not exercise Android bitmap APIs. The new photo regression
tests compiled successfully but still require emulator/device execution.

A fresh debug-signed Preview APK is supplied separately. It does not configure
the production AI endpoint or privacy URL: manual identification and camera
testing work locally, while live Gemini remains unavailable in this build.
No production AAB, physical-phone acceptance, live Gemini acceptance, upload
signing or Play submission is claimed. Existing release gates still apply.

Install acceptance: verify opening the app, retry after camera failure,
permission denial/Settings recovery, capture, retake, rotation and background
recovery on the phone. If Android rejects an update, preserve app data and
compare the installed APK's signing certificate before considering removal.
