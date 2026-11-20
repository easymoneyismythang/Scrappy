# Scrappy 4.9.6 — disconnected AI requests

The gateway previously waited until its provider deadline even after the mobile connection closed. It now aborts the supplied provider signal and releases the request slot when the response connection closes prematurely. A normal completed response leaves the provider signal intact. A started analysis still counts toward the daily budget, and disconnecting does not undo work already received by Google.

Verification: all 21 Node gateway tests passed. Two new real-local-HTTP regression cases verify immediate cancellation and retry capacity even when a custom adapter ignores the abort signal, and verify that normal responses do not trigger cancellation. The existing timeout, rate-limit, invalid-photo, response-validation and budget tests also passed. Provider calls were mocked; no real photo or key was used.

Deployment remains required: this server change takes effect only when the updated backend is hosted. It is not embedded in the Android APK. Custom provider adapters must honor the signal to stop their own work.
