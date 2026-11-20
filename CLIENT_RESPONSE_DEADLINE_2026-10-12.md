# Scrappy 4.9.6 — client response deadline

Continued from source Library version 10, which contains evidence of the previous preview APK build.

## Change
An ordinary socket read timeout restarts whenever data arrives. A slow response could therefore leave AI analysis busy for an unbounded period. The photo client now tracks a monotonic 45-second response budget starting after upload, checks it before and after each body read (including zero-read fallback and EOF), and sets each socket read timeout to the remaining budget. Late results are rejected. Timeout messages direct the user to Retry or the manual helper; the existing activity callback reenables Analyze and Retake.

This is a response budget, not a total upload deadline. Platform socket timeout/disconnect behavior still requires device validation; checks reject late results when a blocking read returns.

## Verification actually performed
- Standalone Kotlin/core suite: 145 passed, no failures.
- Four new tests cover deadline expiry, sub-millisecond rounding, trickle responses, zero-length read fallback/late EOF, and timely short reads.
- AiPhotoClient plus core sources compiled with Kotlin 2.0.21 against Android SDK 36.
- Node gateway suite: 19 passed; mocked provider only.
- Release configuration and document consistency guards: passed.
- Gradle 8.11.1 downloaded and checksum verified. Full testDebugUnitTest/lintDebug/assembleDebug did not start app compilation: com.android.application 8.9.2 could not resolve from configured repositories. See verification/client-response-deadline-2026-10-12/android-build-attempt.log.

No new APK/AAB was produced. The separately delivered earlier preview APK does not contain this source change. This source retains Android versionName 4.9.6 and versionCode 55.

## Required acceptance
1. Rebuild with production-safe test AI endpoint/privacy configuration in a networked Android toolchain.
2. Use a controlled HTTPS test gateway that slowly emits a valid response body. Verify analysis rejects a response exceeding the budget and re-enables Analyze and Retake; manual helper must remain usable. Never use a real provider key in the APK.
3. Verify normal response, no-data timeout, malformed and oversized responses, and retry with one request per consent.
4. Complete physical-phone camera, rotation, permission and background/resume tests in PRELAUNCH_TEST_MATRIX.md.

Production Gemini gateway hosting/configuration, public privacy policy, signing, store assets and Play acceptance remain unfinished.
