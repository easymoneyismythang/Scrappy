# Camera analysis cancellation — October 12, 2026

Closing or recreating CameraActivity previously interrupted its worker without explicitly disconnecting HttpURLConnection. Blocking socket operations could continue, leaving an old request active after Back or rotation.

Added a cancellation handle for each consented request. Destroying the screen disconnects its attached connection. Cancellation before queued processing or after JPEG preparation prevents an upload from starting. Cancellation racing with connection attachment closes that connection immediately. A cancelled response cannot be accepted. Completed requests release the handle, and stale UI callbacks remain guarded by the existing generation check.

Backgrounding alone retains an already consented request; dismissing an unconfirmed consent dialog still prevents sending. Rotation interrupts the old request and restores the local photo. The new screen requires fresh consent to retry analysis.

## Executed verification

- Standalone Kotlin suite: 149 tests passed, including four new cancellation regressions covering active disconnect, cancellation before attachment, completed-request cleanup, and releasing a blocked worker.
- Gateway suite: 21 tests passed with a mocked provider.
- Release configuration and version consistency guards: passed.
- Full Android unit/lint/APK build attempted; Gradle 8.11.1 download failed with Network is unreachable. Android activities and the HTTP integration were not compiled or exercised by the standalone suite. No new APK/AAB was produced.
- Logs: verification/client-cancellation-*.txt.

The earlier Scrappy-v4.9.6-updated-preview.apk remains an older build without this change. Gemini uploads remain disabled in that preview until valid AI and privacy URLs are configured.

## Required integration and phone verification

Rebuild with testDebugUnitTest lintDebug assembleDebug in a configured Java 17 + SDK 36 environment. For AI cancellation acceptance, use a configured HTTPS test gateway with a delayed, mocked provider and a test privacy URL; use only a nonprivate metal photo. After consent, press Back during analysis. Expected: the server observes disconnect/cancellation and releases its active slot; no result appears after leaving. Repeat by rotating during analysis. Expected: local review returns, no automatic upload starts, and a fresh consented retry succeeds. Repeat rotation immediately after consent to exercise cancellation during JPEG preparation. Confirm normal completed analysis still displays its result. Check backend counters, rather than relying only on UI disappearance, to establish network cancellation.

Physical-phone acceptance, actual Android socket cancellation, live Gemini, production hosting/privacy/signing and Play release remain pending.
