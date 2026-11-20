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
