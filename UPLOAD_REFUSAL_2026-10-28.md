# Upload refusal cleanup — October 28, 2026

The gateway previously returned early errors while leaving incomplete request
bodies connected. A client that sent headers and withheld its body could retain
the connection until a general socket timeout after receiving the error.

The shared response helper now marks incomplete uploads for connection closure
and destroys the socket after the JSON response has flushed. This applies to
route, format, size and quota refusals as well as existing upload timeout errors.
Fully received photos continue through the existing analysis path.

Five real TCP regression tests cover missing body bytes for HTTP 404, 415,
oversized 413, chunked 413 and peer-limit 429. They require the complete error
reply, connection closure, no additional provider/budget use, and a successful
health check afterward. The first four cases reproduced the defect by timing
out against the previous response helper; the chunked case adds coverage for
the same refusal path with different HTTP framing.

Verification: all 32 gateway tests and 15 APK-verifier tests passed. Release
consistency and configuration-wiring checks passed. All 173 standalone Kotlin
core/transport tests passed, for 220 automated tests in this session. Gateway output is retained
in verification/upload-refusal-2026-10-28/gateway-tests.txt.

This changes only the Node gateway; no Android APK rebuild was needed or made.
Live Gemini credentials, hosted HTTPS service, phone acceptance, production
signing and Play release remain unfinished. No production service was deployed.
