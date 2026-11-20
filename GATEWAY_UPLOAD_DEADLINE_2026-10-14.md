# Scrappy v4.9.6 — gateway upload deadline

October 14, 2026. Android version remains 4.9.6 (55).

## Confirmed problem and fix
A partial JPEG upload could occupy the gateway concurrency slot beyond the intended receipt deadline. The old Node request/socket timeout settings alone did not promptly free that slot. The new stalled-upload regression was run against the previous source and timed out after 3 seconds with a configured 150 ms test deadline.

The gateway now starts an explicit 15-second wall-clock upload deadline after admitting the request. Occasional bytes do not extend it. A timed-out upload receives HTTP 408, closes its connection, and frees concurrency. No incomplete upload reaches Gemini or consumes daily provider budget. Peer attempt throttling still applies. Completed uploads clear their timer and listeners before analysis. Upload disconnects free capacity too.

## Verification performed in this pass
- Node v24.19.0: all 25 gateway tests passed, zero failures/cancellations/skips.
- Four new regressions: stalled upload, trickling upload, disconnect during upload, and deadline configuration validation.
- Real loopback HTTP/TCP sockets exercised upload receipt and retry after capacity release; provider responses were mocked.
- Node syntax check passed.
- Release configuration and handoff consistency checks passed.
- Evidence: verification/gateway-upload-2026-10-14/gateway-tests.log.

## Limits and next acceptance
No Android source changed; Android compilation, Kotlin suites and APK generation were not rerun. The prior camera-cleanup Android build and tests remain historical evidence for that unchanged client. Its generic non-200 handling already offers retry/manual identification for HTTP 408.

A deployed HTTPS reverse proxy must enforce its own receipt deadlines, particularly when it buffers request bodies. Verify partial and trickling uploads through that proxy, confirm an upload timeout does not consume provider budget, then send a complete JPEG and verify a successful response. Live Gemini credentials/hosting/privacy configuration, device tests and signed Play release acceptance remain pending. No production service or new APK is supplied by this checkpoint.
