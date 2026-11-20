# Scrappy v4.9.6 — current verification record

Verified from the uploaded `Scrappy-v4.9.6-price-safety.zip` on October 6, 2026.

## Freshly verified
- Android metadata is `versionCode 55` / `versionName 4.9.6`; the home/privacy labels also identify v4.9.6.
- `PriceQuotes.confirmedPriceFor` returns only a valid saved yard quote and returns null when none exists.
- `PriceQuotes.estimatePriceFor` is the explicit estimate path and may fall back to the built-in demo benchmark.
- MainActivity uses the explicit estimate API.
- Trip-ledger fallback data remains labeled `Demo benchmark`.
- Node AI gateway regression suite: **12 passed, 0 failed**.

## Fresh Android/core execution boundary
Fresh Android/core execution could not start in this workspace because required dependencies were not locally available and outbound dependency resolution failed:
- standalone core runner: could not download `kotlin-compiler-embeddable-2.0.21.jar`;
- Gradle wrapper: could not resolve `services.gradle.org` for Gradle 8.9.

These are environment/dependency-resolution failures, not failing Scrappy test assertions. They also do **not** establish a fresh Android pass. The included v4.9.0 verification evidence remains historical evidence only.

## Release gate
Before Play release, run the full Android unit/build/lint suite in a networked Java 17 + Android SDK 36 environment, then complete `PRELAUNCH_TEST_MATRIX.md` on a physical Android phone. Production AI hosting/privacy, live-or-clearly-manual price/yard scope, signing, Play Console forms/testing, and final store assets remain required.

## Handoff consistency hardening — October 6, 2026
- Corrected INSTALL_TEST_BUILD.md and NEXT_ACCEPTANCE_MILESTONE.md so current testing targets Android v4.9.6 / versionCode 55 rather than stale v4.9.0/v4.9.1 labels.
- Added `scripts/check_release_consistency.py` to fail when key current handoff docs drift from `app/build.gradle.kts` metadata.
- Consistency guard result: PASS — Android 4.9.6 (54) handoff docs aligned.
- Re-ran Node gateway suite after the documentation/guard change: 12/12 passed.
- Historical v4.9.0 APK/AAB evidence remains historical only and must not be submitted as the current release.
## Release hardening continuation
- `scripts/check_release_config.py`: PASS.
- Release packaging is now gated on safe production AI endpoint + privacy-policy configuration.
- Backend metadata aligned to v4.9.6; gateway suite remains 12/12 passing.
- This does not replace the pending fresh Android/core/lint/device acceptance run.


## Play Store preparation — 2026-10-06
- Confirmed Android `compileSdk` and `targetSdk` are API 36, matching the current Google Play target requirement for new Android phone submissions.
- Replaced stale Play checklist/store listing with v4.9.6 submission materials.
- Added `DATA_SAFETY_WORKSHEET_4.9.6.md` and `PLAY_CONSOLE_SUBMISSION_4.9.6.md`.
- Reworked privacy policy into a publication draft that reflects production AI consent/data flow while retaining required publisher/hosting placeholders for the developer to finalize.
- Removed the historical v4.9.0 unsigned AAB from `build-artifacts` to prevent accidental upload.
- Release configuration and release consistency scripts pass.
- AI gateway tests pass 12/12.
- A fresh Android/Gradle build and signed AAB still require a networked Android build environment, production HTTPS AI/privacy URLs, and developer signing credentials.
