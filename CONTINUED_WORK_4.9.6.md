# Scrappy continued release hardening — 4.9.6 handoff

Date: 2026-10-06

## Fresh verification completed
- `python3 scripts/check_release_consistency.py`: PASS.
- `python3 scripts/check_release_config.py`: PASS.
- `cd backend && npm test`: PASS, 12/12 Node gateway tests.
- Reviewed Android camera/AI consent flow and release configuration gate.

## Verified protections
- Photos remain local until the user explicitly chooses AI analysis and confirms the consent dialog.
- Uploaded image pixels are decoded/re-encoded before sending, removing original EXIF metadata.
- AI upload is capped at 1 MiB and accepts only the configured HTTPS `/v1/identify` endpoint.
- Release packaging refuses missing/unsafe AI endpoint and privacy-policy configuration.
- Gateway validates JPEG input and constrained model output, rate limits requests, enforces a durable daily request budget, and does not expose provider errors to clients.

## Still required before production Play submission
- Run Android unit/lint/build tasks in an environment with the required Gradle/Kotlin dependencies available.
- Deploy the HTTPS AI gateway and perform a real Gemini image request through it.
- Publish/review the production privacy policy and configure its HTTPS URL.
- Complete physical-phone acceptance tests from `PRELAUNCH_TEST_MATRIX.md`.
- Generate and verify the signed release AAB with the production signing key.
- Complete Play Console data-safety/content declarations and submission.

No production Gemini key, signing key, or user photo is included in this source package.
