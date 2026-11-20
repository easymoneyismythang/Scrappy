# Scrappy v4.9.6 — release hardening continuation

This checkpoint adds release-safety controls without claiming a fresh Android binary pass.

## Completed
- Added a Gradle release gate: `assembleRelease` and `bundleRelease` now require a configured HTTPS AI endpoint at `/v1/identify` and a configured HTTPS privacy-policy URL.
- Release URLs reject embedded credentials, query strings, fragments, and non-default TLS ports.
- Debug/preview builds may remain unconfigured so offline/manual testing is still possible.
- Added `.gitignore` rules for Gradle/build output, local properties, signing keys/certificates, and release property files.
- Added `scripts/check_release_config.py` as a lightweight guard for release-gate wiring.
- Aligned AI gateway package/readme metadata to v4.9.6.
- Re-ran release consistency guard and AI gateway tests; both pass, with gateway 12/12 passing.

## Still required before Play release
- Run Android unit/lint/release build in an environment with the pinned dependencies available.
- Configure the production AI gateway URL and a public HTTPS privacy-policy URL.
- Generate a newly signed v4.9.6 AAB; do not upload the historical v4.9.0 AAB in `build-artifacts/`.
- Install a fresh preview/release candidate on a physical Android phone and complete the acceptance matrix.
- Fix any functional defects found there before cosmetic redesign.
