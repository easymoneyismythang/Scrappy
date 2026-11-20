Current source checkpoint — October 29, 2026 (New York): cancellation now reaches photo preparation checkpoints. Added two Android regressions; Android compilation and device tests remain blocked at the Gradle download. 33 Python and 32 gateway tests passed, plus release source checks. No new APK or live Gemini/phone acceptance. See PHOTO_PREPARATION_CANCEL_2026-10-29.md.

# Scrappy next acceptance milestone — v4.9.6 handoff

## Latest verification — October 26, 2026

The build blocker was resolved for this session by installing verified Gradle,
Android SDK and JDK tooling and configuring Java's proxy and certificate store.
The app and camera test APKs now compile. All 154 Android unit and 19 transport
tests pass; lint reports zero errors/fatal issues and 73 warnings. All 27
gateway and seven APK-verifier tests pass. A new debug-signed Preview APK is
supplied separately and signature/package verified. See PHOTO_BOUNDS_2026-10-26.md
and verification/photo-bounds-2026-10-26. This supersedes the earlier build
blocker reports below; the twelve instrumentation tests have not run.

Next: install the new camera/photo-bounds Preview and verify camera permission,
startup retry, capture/retake, orientation and background recovery on a phone.
Production AI/privacy configuration, live Gemini, signing and Play gates remain
unfinished. The new Preview keeps photos local when AI is not configured.

## Earlier verification history

Latest verification: see ANDROID_BUILD_VERIFICATION_2026-10-22.md. The complete Gradle/manifest files are present in the saved source; 27 gateway and 7 APK-verifier tests passed this session. A fresh Android build again failed before compilation at the Gradle download. No new APK was produced.

Latest source checkpoint (October 22): camera startup now rechecks visibility and permission after asynchronous provider initialization. The latest phone screenshot shows the v4.9.6 home screen running; camera and Gemini acceptance remain unverified. Seven verifier regression tests and 27 gateway tests passed again; the Android rebuild is blocked by the Gradle download, so the activity change remains uncompiled. See CAMERA_STARTUP_GUARD_2026-10-22.md. Previous provider error-body cleanup and APK verification are retained.

Prior verification — Updated October 20, 2026 UTC: 173 Kotlin/JVM tests and 25 gateway tests passed. Four added integration tests exercise the shared client against the actual Node gateway with a controlled provider. Fresh Android build is pending because Gradle could not download in this environment. The October 16 permission-recovery APK remains the latest verified APK; it does not include the new HTTP 408 wording. See CLIENT_GATEWAY_INTEGRATION_2026-10-20.md. Live AI configuration and physical-phone acceptance remain pending.

## Historical October 14 verification (predates permission recovery)
- Android metadata remains versionName `4.9.6`, versionCode `55`, package `com.scrappy.app` (`.preview` suffix for debug).
- Standalone Kotlin: 169 tests passed, including cleanup failures, shared HTTP transport, real JVM socket cancellation and overall upload deadline.
- Gateway: 21 mocked-provider tests passed. Release configuration and version consistency checks passed.
- Fresh Android verification passed: 154 Android unit tests + 15 host JVM transport tests, lintDebug and assembleDebug. The separately supplied Scrappy-v4.9.6-camera-cleanup-preview.apk includes the current fixes. No production APK/AAB was built.
- Any older v4.9.0 AAB/APK in historical artifacts is not a current release candidate and must not be uploaded to Play.
- See CAMERA_CLEANUP_BUILD_2026-10-14.md for current evidence; REQUEST_DEADLINE_2026-10-14.md retains slow-upload device verification criteria.

## Phone acceptance gate
Install the separately supplied Scrappy-v4.9.6-permission-recovery-preview.apk, then test on a physical Android phone in this order:
1. Launch/home navigation and daily verse.
2. Deny Camera permission, open Android Settings → Apps → Scrappy Preview → Permissions → Camera and allow it, then return through Recents. Expect a live preview and enabled Capture without reopening the screen. Repeat Settings round trips and confirm no duplicate-binding failures. Return from Settings while reviewing a photo and confirm review is preserved.
3. Capture a portrait metal photo; verify review image orientation.
4. Retake; verify the old cached photo/result is discarded.
5. Rotate/recreate or background/restore during photo review; verify the local photo/result survives correctly.
6. Confirm AI stays disabled/local when no HTTPS endpoint/privacy URL is configured.
7. With a test AI service configured, verify per-photo consent, successful result, timeout/error fallback, and no automatic quote/ledger write.
8. Add/edit/delete yard-grade quotes; restart app and verify persistence and source/provenance labels.
9. Enter a load in yard comparison; verify payout, travel/fuel cost, net value and tie handling.
10. Add/edit/delete ledger trips; export/import a backup and verify totals.
11. Configure weather place; verify weather and NOAA radar load, refresh and offline failure message.
12. Test document picker/share flows and app background/restore behavior.

## Release blockers after phone acceptance
- Deploy and validate the Gemini gateway with production credentials, quotas, abuse controls and retention policy.
- Publish a real privacy-policy URL with developer/support identity.
- Replace/augment demo yard data with a verified local-business source, or keep the product clearly manual/reference-only.
- Establish a reviewed source for current scrap pricing, or clearly label benchmark/manual prices as non-live.
- Review weather/radar production suitability.
- Create final branding/store assets after functional acceptance.
- Create/secure upload signing key; build signed v4.9.6-or-later AAB.
- Complete Play Console data-safety/content forms, testing track and submission.

Do not claim Play Store readiness until these gates are complete.
