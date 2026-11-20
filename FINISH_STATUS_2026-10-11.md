# Scrappy v4.9.6 — completion status

Continued from the latest camera/weather fixes package, preserving the newer version 4 camera follow-up.

## Completed in this session
- Preserved existing camera/weather fixes and the newer capture-rotation, capture-start error recovery and single-consent-dialog fixes.
- Closed a release validation bypass: the shared preReleaseBuild task now requires the production AI/privacy configuration check, covering direct release packaging tasks.
- Gateway startup rejects invalid request-limit configuration and unusable service adapters.
- Added regression tests for concurrency limits, provider failure recovery, budget preservation for rejected work, and oversized/malformed provider replies.
- GitLab pipeline runs the gateway suite before the APK job; the APK job also checks release configuration wiring and document consistency.
- Corrected the current README to state that this source ZIP contains no APK/AAB.

## Executed verification
- Node gateway: 15 tests passed, zero failures. Tests use a fake provider, not live Gemini.
- Release configuration wiring check: passed.
- Release/document consistency: passed, Android version 4.9.6 / versionCode 55.
- Android unit tests, lint and APK build: blocked before compilation. SDK 36 is available, but Gradle 8.11.1 is absent and its download failed with Network is unreachable. No fresh Android pass or installable build is claimed.

## Required to finish
1. Run the included cloud pipeline in the user's repository to build a current APK and execute Android tests/lint.
2. Install that APK on an Android phone and complete PRELAUNCH_TEST_MATRIX.md.
3. Provision the Gemini gateway and public privacy policy, configure production URLs, and test real analysis. Credentials belong on the server.
4. Finalize product scope, branding, signing and Play Console testing/submission. Current yard contacts and quotes are manual; prices are not a live feed. Billing/ads are not implemented.

Historical v4.9.0 build records do not verify this source. This package is continued development work, not a finished or published app.

## Camera recovery continuation — October 12 UTC
See CAMERA_RECOVERY_CHECK_2026-10-12.md for the latest source changes. Capture errors now expose the manual helper, and leaving the screen dismisses unanswered AI consent. Gateway tests passed 19/19, standalone Kotlin core tests passed 137/137, and release guards passed. Android compilation remains blocked by the Gradle download; these camera changes are uncompiled and device acceptance is pending.
