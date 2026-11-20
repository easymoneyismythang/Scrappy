# Scrappy v4.8.0 — ten yard comparison source milestones

Open Earnings & Profit Tools → Compare yards for my current load. Add metal to the current load and enter yard quotes in Scrap Price Guide first. Select each yard, enter its round-trip travel costs, and tap Add or update selected yard. Up to 20 yards may be compared. Updating a yard replaces its comparison inputs; removing or clearing affects only the comparison.

| # | Completed source capability | Verification |
|---|---|---|
| 1 | Discover saved quote yards with case/whitespace normalization | yardListNormalizesDuplicatesAndRejectsBadQuotes |
| 2 | Value every load grade using the latest quote from that specific yard | wholeLoadUsesYardQuotesAndPreservesOriginal; latestQuoteIsScopedToNormalizedYard |
| 3 | Show missing grades and exclude incomplete yards from ranking | missingGradeDoesNotFallBackToDemoOrOtherYard; incompleteYardSortsAfterNegativeNet |
| 4 | Enter per-yard round-trip miles, MPG and fuel price | costsAndBreakEvenIncludeFuelLaborAndFees; invalidAndDuplicateTripsRejected |
| 5 | Include per-yard travel labor and fees in estimated net | costsAndBreakEvenIncludeFuelLaborAndFees |
| 6 | Rank complete yards by estimated net with stable ties | ranksByNetRatherThanGross; tiesHaveStableAlphabeticOrder |
| 7 | Show dollar difference from the highest estimated net | reportContainsSpreadAndProvenance |
| 8 | Show each yard's break-even payout | costsAndBreakEvenIncludeFuelLaborAndFees |
| 9 | Show stale/future quote flags and grade-level price/date provenance | flagsStaleAtExactly24Hours; futureQuoteIsExplicitAndNotStale; reportContainsSpreadAndProvenance |
| 10 | Share comparison snapshots and restore comparison inputs after activity recreation | Report model tested; Android share/recreation checks require a phone |

Quotes are manually entered snapshots, not live yard offers. Future-dated and at-least-24-hour-old quotes remain visible with confirmation flags. Quotes never fall back to another yard or demo benchmarks. Rankings are estimates, including loss-making trips. No yard acceptance or route distance is inferred. All mileages are user entries. Fuel, labor and fees are included; wear, taxes and unentered expenses are excluded. Comparisons do not write to the ledger or reprice saved estimates/receipts. Yard quotes and comparison inputs are not included in portable ledger backups. Comparison inputs last for the current screen session, including Android saved-state recreation; they are not a new persistent ledger record.

15 new core regression tests; full suite 123 tests. See IMPLEMENTATION_STATUS.md for final Android build evidence and remaining release work.
