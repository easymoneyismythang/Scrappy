# Scrappy v4.9.6 — provider error cleanup

October 20, 2026 (America/New_York). Source checkpoint; Android version remains 4.9.6 (55).

## Fix
The Gemini adapter previously threw on non-success HTTP status without closing the unread response body. It now cancels that stream before reporting the generic provider-unavailable error, releasing resources on repeated provider failures. Cleanup errors do not expose provider details.

## Verification performed in this pass
- New regression test failed before the fix: HTTP 400 left its response stream uncancelled.
- All 27 Node gateway tests passed after the fix (including HTTP 400, 429, 500 and 503 stream cleanup, and cleanup failure redaction).
- Release configuration and version consistency scripts passed.
- Android test/lint/build attempted; Gradle 8.11.1 download failed with Network is unreachable. No new APK or Android verification completed.
- Kotlin/JVM tests were not rerun in this pass; the prior 173-test result is historical evidence. No Kotlin or Android source changed in this pass.

Tests use controlled providers and synthetic photo bytes. Live Gemini and physical-phone acceptance remain unverified.

## Next verification
Build the current source using the included CI workflow or an Android build environment: `sh gradlew --no-daemon --console=plain testDebugUnitTest lintDebug assembleDebug`. Then test camera capture, consent, successful analysis and retry after an AI error on a phone using a configured test gateway. Deploy the updated backend to exercise this fix with Gemini; updating the APK alone does not update the server.

See verification/provider-body-cleanup-2026-10-20/ for this pass's logs.
