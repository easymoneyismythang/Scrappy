# Scrappy overall photo request deadline — October 14, 2026 (UTC)

## Fix
The 45-second response budget began after the JPEG upload. HttpURLConnection connect/read timeouts did not cover a blocked output write, so a slow upload could leave the camera analysis worker waiting indefinitely while the screen stayed busy.

Added a 60-second watchdog covering connection/upload/response work after connection creation. It attempts to disconnect an expired request on a dedicated daemon thread. Monotonic deadline checks after upload, around response reads and before accepting parsed results also reject late results if an adapter ignores disconnect. The existing 15-second connect and 45-second response limits remain.

The watchdog is cancelled and its executor shut down on every completed/failed request. Its disconnect callback only references that request's connection. One slow disconnect does not block other requests' watchdogs. A timeout-triggered IOException is translated into the existing readable retry/manual-helper message; explicit user cancellation takes precedence. No automatic retry or upload was added.

## Verification executed
- Standalone Kotlin suite: 164 tests passed, including the previous 158 and 6 deadline tests.
- New tests cover a blocked upload, a real loopback server withholding response headers, a late body from an adapter ignoring disconnect, cancelling the watchdog on completion, a disconnect callback throwing, and invalid timeout configuration.
- Earlier HTTP cancellation/redirect/body-limit tests also pass in this run.
- Release configuration and version consistency guards passed. The handoff check first caught a stale-artifact warning accidentally removed in the previous documentation update; it was restored before the final passing check.
- Evidence: verification/request-deadline-2026-10-14-core.log, release.log and source-sha256.json.

## Remaining acceptance
No new Android build was attempted after the already confirmed Gradle 8.11.1 download failure in this session. That failure is recorded in verification/client-http-2026-10-14-android.log. No new APK/AAB was produced. Backend code is unchanged from the prior 21-test gateway pass.

JVM tests do not establish Android socket or physical-phone behavior. Disconnect is a best-effort platform operation, not a guarantee that a remote provider has stopped. A fresh Android build, slow-upload phone testing and deployed gateway/provider cancellation checks remain required. On a configured test gateway, throttle a consented nonprivate photo upload; expect timeout recovery near the overall budget, no stale result or automatic retry, and a fresh consented retry to work. Also repeat normal analysis, Back and rotation cases. Confirm gateway capacity is released using its own test instrumentation.

Production hosting, live Gemini, public privacy configuration, signing and Play acceptance remain pending. Prior preview APKs lack this and the later source cancellation changes.
