# Scrappy camera AI gateway — v4.9.6

This server and Android client implement the upload/response path. No production server, Gemini credentials or live model call was provisioned or tested in this session. This source package contains no APK. Preview builds leave uploads disabled unless configured. It is not a deployed production service.

## Run and configure
Use Node 22 or newer. No npm packages are required. Run `npm test` in this folder. Put `GEMINI_API_KEY` in the server's secret manager/environment, never in Android or this ZIP. Set `GEMINI_MODEL` to an available vision model that supports GenerateContent JSON schema in your account. No default model is assumed. Set `BUDGET_FILE` to an absolute file on a persistent, private disk and `DAILY_REQUEST_LIMIT` (default 100, maximum 10,000). Then run `npm start`. The server listens on 127.0.0.1:8080 by default; set HOST/PORT for your hosting environment. It refuses to start without provider configuration and a durable budget-file path.

Route HTTPS `POST /v1/identify` to the server. The app sends a JPEG body with a known Content-Length, at most 1 MiB. GET /health is a process liveness check; it does not prove provider connectivity. The server forwards inline image bytes to Google's GenerateContent API, requests a constrained metal-group schema and returns validated data. No image, prompt or model-response files/logs are created by this server. It persists only UTC day and request count. A request attempt consumes budget even if the provider fails. Per-peer memory rate limits default to four attempts/minute, with two concurrent requests and a bounded peer table. Forwarded IP headers are ignored.

## Deployment gates
Early HTTP refusals (including wrong route, unsupported format, oversize/unsupported chunked bodies and request limits) flush their JSON reply and close the connection when the upload is incomplete. A rejected client cannot keep an abandoned upload connection open until the general socket timeout. Fully received requests retain the existing connection behavior.

Photo receipt has a separate 15-second wall-clock deadline beginning when an admitted request's headers arrive. Stalled uploads and uploads that keep trickling bytes receive a generic HTTP 408 response; their connection is closed and their concurrency slot is freed. An incomplete upload never starts Gemini or consumes the daily provider budget, but it counts against the peer attempt throttle. Upload disconnects also release capacity. The `createServer` test/embedding option `uploadTimeoutMs` may shorten this deadline (integer 1–15,000); it is not an environment variable. Configure equivalent limits at the reverse proxy, since buffering there can hide a slow client from this server. This deadline does not include TLS or header receipt at the proxy.

The gateway bounds each analysis to 30 seconds, returns a generic 503 on timeout and sends an abort signal to the Gemini adapter. Custom adapters must honor the supplied signal to stop their underlying work. Timed-out attempts still consume the daily budget; late replies are discarded. If the client disconnects while awaiting analysis, the gateway also aborts the provider signal and releases the request slot immediately. A started analysis still consumes budget; disconnecting cannot retract work already received by the provider. This deadline covers analysis after receipt of the JPEG, separately from the upload timeout.

Use exactly one process/replica with the same durable budget file; this file counter is not a distributed quota. Restarting with a new/ephemeral file resets the budget. Put a TLS reverse proxy/API gateway in front, configure body/time limits and abuse controls, and disable body logging at the hosting/proxy layers. The request-count limit is not a dollar billing limit; configure provider billing alerts/limits separately. Behind a proxy, all users may share the peer throttle; configure gateway authentication/attestation and user quotas before broad public rollout. The supplied gateway is suitable for limited testing; production abuse protection and hosting acceptance remain operator tasks.

Review the selected Google account's retention, data-use and commercial terms; publish the reviewed privacy policy. Do not assume images are excluded from Google's retention/training merely because this server does not store them. No user's real photo or API key was used in automated tests.

Build Android with `-PscrappyAiEndpoint=https://YOUR_DOMAIN/v1/identify -PscrappyAiPrivacyUrl=https://YOUR_DOMAIN/privacy`. These are public URLs, never keys. Both default to empty. The app accepts only HTTPS, does not follow upload redirects, excludes original EXIF metadata by pixel re-encoding, caps response size, and asks for consent on every request. Anything visible in the photo remains visible to the provider. Leaving the screen does not retract a request already sent. Model replies are temporary broad visual suggestions; no automatic grade, quote or financial record is created.

## Acceptance before enabling uploads
Test a real Gemini request through the deployed HTTPS service; invalid/unrecognizable/nonmetal images; refusal/incomplete response; upload timeout/408 and analysis timeout/429/503; actual EXIF removal and rotation; declined consent; retake/back during upload; process recreation; privacy policy link; hosting logs/retention; quotas across restart; provider credentials rotation. Run the full phone acceptance matrix. None of these production/hardware checks is claimed as complete.

Official API references checked October 3, 2026:
- https://ai.google.dev/api/generate-content
- https://ai.google.dev/gemini-api/docs/generate-content/image-understanding
- https://ai.google.dev/gemini-api/docs/structured-output
