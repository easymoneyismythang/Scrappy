# Scrappy photo HTTP verification — October 14, 2026 (UTC)

## Changes
Moved the existing HTTP upload/response implementation from Android bitmap code into AiHttpClient. AiPhotoClient still prepares metadata-free JPEG pixels and delegates to the same transport. Production endpoint validation, payload/response limits, timeouts, consent flow and disabled-by-default configuration are preserved.

Added cancellation checkpoints after upload and before/after body reads. If cancellation happens as the upload closes or headers arrive, the client stops before waiting on or reading more response data, even when a connection adapter does not honor disconnect immediately.

## Executed checks
- Standalone JVM Kotlin suite: 158 tests passed (149 prior tests plus 9 HTTP-client tests).
- Real loopback HTTP tests cover exact POST bytes/headers, refusing redirects, oversized chunked responses, malformed responses, and releasing a real socket worker with cancellation while the server withholds headers.
- Controlled connection tests cover no connection for unsafe endpoints/empty payloads/pre-cancelled requests, cancellation after upload/headers, 429/503/500 handling without reading error bodies, timeout messaging, and cleanup.
- Gateway suite: 21 tests passed using a mocked provider.
- Release configuration and version consistency checks passed.
- Full Android testDebugUnitTest lintDebug assembleDebug was attempted but failed downloading Gradle 8.11.1 with Network is unreachable. No APK/AAB was generated.

Evidence: verification/client-http-2026-10-14-*.log and source hash manifest.

## Limits and next acceptance
The HTTP implementation now compiles and runs in the standalone suite, but Android bitmap preparation, Activity integration and Android's HttpURLConnection implementation have not been freshly compiled or exercised here. JVM socket cancellation does not establish Android device acceptance or server-side provider cancellation across deployed networks.

Rebuild the latest source with Java 17, SDK 36 and Gradle dependency access. Run testDebugUnitTest lintDebug assembleDebug. Configure a controlled HTTPS gateway/privacy URL for AI phone testing. On a nonprivate metal photo, consent and press Back or rotate during analysis; verify no stale result, no automatic retry, and gateway cancellation releases capacity. Confirm a fresh consented retry succeeds.

The previous updated preview APK lacks the later cancellation changes and this transport update. The source ZIP is not installable. Production Gemini hosting/privacy/signing, live-provider testing and Play acceptance remain pending.
