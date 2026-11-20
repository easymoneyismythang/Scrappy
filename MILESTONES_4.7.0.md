# Scrappy v4.7.0 — next 50 source milestones

This batch defines 50 concrete source capabilities after v4.6.0, grouped into five ten-item workstreams. All are implemented in source. They are not 50 verified release gates: Android runtime acceptance checks remain pending. Production Gemini, live prices and Play publication are still separate unresolved work.

Open **Earnings & Profit Tools** on the home screen, or the tools button under Mixed Loads, Trips & Sales. Record correction/rename/copy controls are on their existing ledger screens. Backup merge and recovery controls are under Backup & restore ledger.

| # | Source milestone | Implementation and verification |
|---|---|---|
| 1 | Inclusive custom-date earnings reports | SalesAnalytics.filter; dateFilterIsInclusive/endDateUsesLocalMidnight |
| 2 | Exact receiving-yard filter | SalesAnalytics.filter; exactYardIgnoresCaseAndWhitespace |
| 3 | Receipt sorting by date, payout or cash net | SaleSort; newestSort/oldestSort/netSortAllowsLosses/payoutSort |
| 4 | Monday-start weekly earnings groups | SalesAnalytics.periods; weeklyGroupsStartMondayAcrossYear |
| 5 | Monthly earnings groups in local time | SalesAnalytics.periods; monthGroupsUseLocalDate |
| 6 | Yearly earnings groups | SalesAnalytics.periods; yearGroupsNewestFirst |
| 7 | Yard earnings ranked by cash net | SalesAnalytics.yards; yardGroupingNormalizesNames |
| 8 | Average payout and cash net per receipt | Earnings; averagesIncludeLosses |
| 9 | Cash net and expenses per recorded mile | Earnings; perMileCalculations/zeroDenominatorsAreUnavailable |
| 10 | Payout variance in dollars and percent | Earnings; accuracyAndDstBoundary |
| 11 | Hypothetical price sensitivity preview | ProfitPlanner.scenario; priceScenarioChangesNetAndKeepsOriginal |
| 12 | Fuel-price sensitivity preview | ProfitPlanner.scenario; fuelScenario |
| 13 | Round-trip distance sensitivity preview | ProfitPlanner.scenario; distanceScenario |
| 14 | Target-net required payout | ProfitPlanner.targets; targetNetRequiresAllCosts |
| 15 | Target-net required average price | ProfitPlanner.targets; requiredAveragePrice |
| 16 | Required weight at unchanged metal mix | ProfitPlanner.targets; requiredWeightAssumesFixedMix |
| 17 | Maximum fuel-affordable round-trip miles | ProfitPlanner.targets; maxRoundTripMiles/infeasibleOrZeroFuelDistanceUnavailable |
| 18 | Vehicle load-capacity comparison | ProfitPlanner.capacity; capacityCanExceed100 |
| 19 | Current load subtotals by metal grade | ProfitPlanner.metals; gradeBreakdown |
| 20 | Load quote-source quality summary | ProfitPlanner.quotes; quoteReviewDistinguishesDemoOldAndFuture |
| 21 | Rename saved estimates | LedgerTools.rename; renameRetainsIdentityAndSnapshot |
| 22 | Duplicate saved estimates with fresh IDs | LedgerTools.duplicate; duplicateGetsNewIdsAndCapturedPrices |
| 23 | Correct existing sale receipts | LedgerTools.correct; correctionPreservesHistoricalGrossAndLabor |
| 24 | Reject future dates on receipt save/correction | TripLedger.receipt and LedgerTools.correct; correctionRejectsFutureDate |
| 25 | Share individual sale receipts | LedgerActivity.showSales share action; device chooser check pending |
| 26 | Export date/yard-filtered receipts as CSV | WorkbenchActivity.reports; filter and CSV tests; picker check pending |
| 27 | Search receipt yard, ID or linked estimate ID | SalesAnalytics.filter; searchMatchesReceiptId/searchMatchesEstimateId/searchMatchesYard |
| 28 | Share complete ledger earnings/load summary | WorkbenchActivity.home; model totals tested, chooser check pending |
| 29 | One-step deletion undo until next ledger change | LedgerStore.deleteWithUndo/undoDelete; persistence check pending |
| 30 | Visible ledger capacity counters | WorkbenchActivity.home/LedgerActivity; device display check pending |
| 31 | Backup merge preview with add/skip counts | LedgerTools.merge; mergeAddsAndIsIdempotent |
| 32 | Idempotent backup merge skips identical records | LedgerTools.merge; mergeAddsAndIsIdempotent |
| 33 | Reject conflicting backup record IDs | LedgerTools.merge; mergeRejectsConflictingEstimateAndSaleIds |
| 34 | Reject over-capacity merges before writing | LedgerTools.merge; mergeRejectsOverCapacity |
| 35 | Merge while preserving current load | LedgerTools.merge default; mergeDraftChoiceIsExplicit |
| 36 | Explicit merge option to use imported load | LedgerTools.merge option; mergeDraftChoiceIsExplicit |
| 37 | Strict UTF-8 backup decoding including BOM | LedgerTools.strictUtf8; utf8BomAndMalformedBytes |
| 38 | Spreadsheet CSV Unicode BOM and formula protection | LedgerTools.csvForSpreadsheet; spreadsheetCsvHasBomAndNeutralizesFormula |
| 39 | Snapshot exports survive picker recreation without large Bundles | ExportSnapshot; source inspected; Android recreation check pending |
| 40 | Pre-import recovery checkpoint | LedgerStore.restore/recoverBeforeImport; persistence check pending |
| 41 | Scale all current-load weights | LedgerTools.scale; scaleAndCapacityBounds |
| 42 | Duplicate a current-load row | LedgerTools.copyItem; copyRowAndRejectFullDraft |
| 43 | Confirmed repricing from saved yard quotes | LedgerTools.reprice; repriceRequiresEveryGradeAndRetainsWeight |
| 44 | Combine rows with identical quote provenance | LedgerTools.consolidate; consolidateOnlyIdenticalQuoteProvenance |
| 45 | Sort current-load rows by metal | LedgerTools.sorted; sortAndClearCostsKeepItems |
| 46 | Reset trip costs without deleting load rows | LedgerTools.clearCosts; sortAndClearCostsKeepItems |
| 47 | Pounds and kilograms in load summaries | WorkbenchActivity.loadText; existing pounds/kilograms core tests |
| 48 | Review missing, stale and future quote dates | ProfitPlanner.quotes; quoteReviewDistinguishesDemoOldAndFuture |
| 49 | Changing weight unit converts entered quantity | LedgerActivity.showItem; existing conversion tests, spinner check pending |
| 50 | Share current applied load with captured price sources | WorkbenchActivity.loadTools; load/model tests, chooser check pending |

Core verification: 108 JUnit tests passed, including 50 new tests in SalesAnalyticsTest, ProfitPlannerTest and LedgerToolsTest. These exercise pure models; they do not validate Android persistence or UI actions. Android Gradle tests (108), debug APK, lint (zero errors) and minified unsigned release compilation also passed. See IMPLEMENTATION_STATUS.md for evidence and phone checks.

Record formats remain compatible with v4.6.0 ledger backups. Corrections preserve historical estimated gross/labor; modeled fuel scales using the original per-mile rate. If original miles were zero, edited modeled fuel remains zero. Repricing affects only the current draft and requires a saved quote for every grade. What-if previews never write to the ledger.

Deletion undo is one checkpoint, replaced by another deletion and cleared by any subsequent ledger write. Pre-import recovery is one separate checkpoint; it persists until the next import/recovery and is excluded from portable backups. Recovery replaces the whole ledger, so the user is prompted to export first. Checkpoints and cached exports contain financial data.
