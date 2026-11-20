# Scrappy v4.9.6 — live setup diagnostic

Added `python3 scripts/build_phone_test.py --check-service` and manual
GitHub/GitLab jobs to check the configured gateway and privacy page before
building an APK. The diagnostic performs GET requests only, checks a small
health response, reads one byte of HTML from the privacy page, verifies TLS
using Python's normal HTTPS handler and refuses redirects. Errors identify
the failed check without printing URLs or underlying proxy/network errors.
It never sends photos, credentials or analysis requests to Gemini.

Verification: 33 Python tests and 32 gateway tests passed (65 total).
Nine new tests cover health/privacy requests, missing setup, invalid or
oversized health replies, missing/incorrect privacy content, redacted errors,
HTTP failure, CLI exit status/no build and redirect-handler behavior.
These diagnostic tests use controlled responses; no deployed service was
contacted. Existing gateway tests exercise loopback HTTP with mocked Gemini.

Release source checks passed. Workflow YAML parsed successfully.
No Android source changed. No new APK, Android compilation, physical phone
test, hosting deployment or live Gemini acceptance is claimed. Actual public
service addresses and server-side Gemini setup remain necessary.
