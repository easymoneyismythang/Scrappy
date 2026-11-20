# Current Scrappy v4.9.6 checkpoint

Current checkpoint — October 14, 2026 (UTC): camera cleanup/cancellation fixes and a fresh Android Preview APK. Gradle testDebugUnitTest, lintDebug and assembleDebug passed. Tests: 154 Android + 15 JVM transport = 169 passed; standalone Kotlin also 169 passed; gateway 21 passed with a mocked provider. Lint: zero errors/fatal issues, 79 warnings. Preview APK signature verified. AI endpoint/privacy fields are empty; live Gemini and physical-phone acceptance remain pending. See CAMERA_CLEANUP_BUILD_2026-10-14.md. Earlier build-blocked notes are historical.

## Historical record below

# Scrappy v4.9.6 — price provenance safety checkpoint

## Source changes
- Added `PriceQuotes.confirmedPriceFor`: returns only a valid user-entered yard quote and returns null when none exists.
- Added `PriceQuotes.estimatePriceFor`: explicitly names the demo-benchmark fallback behavior used by estimate calculators.
- Deprecated ambiguous `priceFor` to prevent new code from accidentally presenting a demo benchmark as a confirmed market quote.
- Updated MainActivity to call the explicit estimate API. Existing UI disclosure remains: metals without a saved quote use demo benchmarks and are not a live market feed.
- Added a regression test proving confirmed-price lookup never returns demo data.
- Advanced Android metadata to versionCode 55 / versionName 4.9.6 and synchronized visible version labels.

## Verification boundary
- Source/static checks and backend tests can run in this workspace.
- Android Gradle/JUnit execution remains dependent on the unavailable/corrupt local Gradle/Kotlin dependency cache; do not claim a fresh Android suite pass until it runs successfully.
- Physical Samsung acceptance testing remains required before release.
