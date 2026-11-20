# Scrappy v4.9.6 — Release Candidate Handoff

Status: source-level release candidate. This package is the strongest verified handoff produced in the current workspace; it is **not** represented as Play-publishable until the fresh Android build and physical-device acceptance gates below are completed.

## Current verified gates

- Android metadata aligned at versionName 4.9.6 / versionCode 55.
- Release configuration safety gate passes static verification.
- Release-document consistency gate passes.
- AI gateway automated suite passes 32/32 in the November 3, 2026 checkpoint (controlled test provider; not live Gemini).
- Latest camera photo-preparation cancellation hardening is covered by 20 source safety invariants and 39 Python helper/verifier tests.
- No TODO/FIXME/NotImplemented/UnsupportedOperationException markers were found under current app/backend source.
- Android manifest disables cleartext traffic and backups; non-launcher activities are non-exported.
- Release packaging is configured to require HTTPS production AI and privacy-policy configuration.

## Current build blocker

The November 3, 2026 fresh Android camera-test build could not start because the Gradle 8.11.1 distribution host (`services.gradle.org`) failed DNS resolution in the current workspace. The latest Kotlin change therefore remains uncompiled. The repository `easymoneyismythang/Scrappy-420` was verified through the GitHub connector as public and size 0, so it does not contain this source checkpoint.

## External gates still required

1. Fresh Gradle sync/build/test/lint using Java 17 + Android SDK 36 with network/dependency access.
2. Configure and deploy the production AI gateway with a server-side Gemini credential and HTTPS endpoint.
3. Publish the final public privacy-policy URL and configure it in the release build.
4. Install the freshly generated v4.9.6 build on physical Android hardware and complete the prelaunch/acceptance matrix, including camera, AI consent/fallback, weather/radar, yards/calling, prices, ledger/import/export/undo, calculations and rotation/relaunch behavior.
5. Fix any device-test defects, then generate and sign the final AAB with the owner's protected Google Play upload key.
6. Complete Play Console listing, Data safety/privacy declarations, screenshots/assets, testing track requirements and final review.

The historical v4.9.0 AAB under `build-artifacts/` must not be uploaded as the v4.9.6 release.
