Current source checkpoint — November 3, 2026: cancellation is now passed into photo decoding, with cancellation checks before/after bounds decoding, after pixel decoding, and after EXIF orientation lookup. Offline camera-safety source guard covers 20 consent, cancellation, stale-result, photo-cleanup, EXIF and upload-size invariants. Python helper/verifier suite: 39 passed; gateway suite: 32 passed; release consistency/configuration checks: passed. Fresh Android camera-test build was attempted but Gradle distribution download failed because `services.gradle.org` could not be resolved. Android compilation/instrumentation, live Gemini and physical-phone acceptance remain unverified; no new APK/AAB produced. See CONTINUED_WORK_2026-11-03.md. GitHub `easymoneyismythang/Scrappy-420` previously reported size 0, so these source changes have not been pushed there.

Current source checkpoint — October 29, 2026 (New York): cancellation now reaches photo preparation checkpoints. Added two Android regressions; Android compilation and device tests remain blocked at the Gradle download. 33 Python and 32 gateway tests passed, plus release source checks. No new APK or live Gemini/phone acceptance. See PHOTO_PREPARATION_CANCEL_2026-10-29.md.

> Latest source checkpoint — October 28, 2026 (New York): added Cancel analysis with photo preservation, request cancellation and stale-result protection, plus one device test. Android build blocked at Gradle download (Network is unreachable); Android code/test remain uncompiled and unrun. No new APK. See CAMERA_CANCEL_2026-10-28.md.

> Latest source checkpoint — October 28, 2026 (New York): added a read-only live setup diagnostic and manual CI jobs. 33 Python and 32 gateway tests passed; diagnostic network responses were mocked. No new APK or live Gemini/phone acceptance. See LIVE_SETUP_CHECK_2026-10-28.md.

> Latest source checkpoint — October 23, 2026 (New York): added nine Android instrumentation tests for camera lifecycle and photo processing, plus a manual emulator workflow. These tests have NOT been compiled/run: Gradle download remains blocked. Seven existing APK-verifier tests and release source checks passed. GitHub repository search returned no accessible Scrappy repository. No new APK or phone/live-Gemini acceptance. See ANDROID_CAMERA_AUTOMATION_2026-10-23.md.

> Latest source checkpoint — October 23, 2026 (New York): synchronous camera-provider startup failure is now caught. 27 gateway and 7 APK-verifier tests passed; these do not exercise the camera change. Android compilation remains blocked by Gradle network access. No new APK or phone/live-AI acceptance. See CAMERA_PROVIDER_RECOVERY_2026-10-23.md.

# Scrappy v4.9.6 — current verification record

Current source checkpoint — October 20, 2026 UTC, versionCode 55: 173 Kotlin/JVM tests and 25 Node gateway tests passed, including four new client/gateway integration tests. HTTP 408 now explains upload timeout and retry. These tests use a controlled provider on loopback, not live Gemini. Fresh Android build/lint was attempted but Gradle download failed with Network is unreachable. No new APK/AAB was produced. See CLIENT_GATEWAY_INTEGRATION_2026-10-20.md and verification/client-gateway-2026-10-20/.

## Historical Android checkpoint — October 16, 2026 (UTC)

The records below apply to the October 16 source/APK, before the HTTP 408 message and integration additions.

## Executed on October 16 source
- Gradle testDebugUnitTest lintDebug assembleDebug: BUILD SUCCESSFUL.
- 154 Android unit tests + 15 host JVM HTTP transport tests: 169 passed, zero failures/errors/skips.
- lintDebug: zero errors/fatal issues, 79 warnings.
- Preview APK v2 signature verified; package com.scrappy.app.preview, versionCode 55, versionName 4.9.6; minSdk 26 / targetSdk 36.
- Camera startup recovery field confirmed in packaged DEX. This is packaging verification, not runtime/device acceptance.
- Gateway suite: 25/25 passed earlier on the same unchanged gateway/test source; five focused upload-close regression runs passed. These are mocked-provider tests with real loopback sockets, not live Gemini.
- Release configuration and handoff consistency checks passed after the document update.

## Limits
AI_ENDPOINT and AI_PRIVACY_URL remain empty in this Preview. No live Gemini request, production service validation, physical-device test or signed production APK/AAB was performed. Standalone Kotlin tests were not rerun separately; equivalent current Android/JVM Gradle suites passed. The earlier network/JDK/certificate setup blockers were resolved for this build. Historical failed setup logs are retained as environment evidence, not current failures.

Current evidence: verification/permission-build-2026-10-16/ and verification/permission-recovery-2026-10-16/. See ANDROID_PERMISSION_BUILD_2026-10-16.md for APK hash and phone acceptance steps.

Current version: 4.9.7 — dark ScrapRight-style UI redesign (Prices / Scan / Yards / More tabs, real Covington yards, Georgia index prices).
