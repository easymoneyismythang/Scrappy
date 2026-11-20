# Next 15 milestones — v4.5.0

All 15 are implemented in source. This is an implementation batch, not 15 verified Android release gates. Core tests passed; Android UI/build verification remains pending.

| # | Milestone | Delivered | Verification |
|---|---|---|---|
| 1 | Mixed-metal load builder | Add up to 100 grade-specific rows; summed weight and gross. | Mixed-load gross regression test. |
| 2 | Pound/kilogram input | Accept either unit and normalize to pounds. | Conversion and invalid-unit tests. |
| 3 | Captured quote provenance | Keep price, yard and quote time per item, including explicit demo fallback. | Captured-price and zero-quote tests. |
| 4 | Load item editing/removal | Change weight retaining original quote; confirm removal. | Model copy tests; device actions pending. |
| 5 | Persistent working draft | Save added items and applied trip costs on the device. | Draft JSON round-trip tests; Android persistence pending. |
| 6 | Mileage and fuel accounting | Use round-trip miles, MPG and gas price; retain actual receipt miles and modeled fuel. | Fuel and actual-mile tests; device persistence pending. |
| 7 | Travel labor accounting | Apply total travel minutes and hourly labor value. | Labor/net regression tests. |
| 8 | Trip fees | Include additional trip fees with validated inputs. | Fees/net and invalid-cost tests. |
| 9 | Net and break-even report | Display gross, each cost, net and required break-even payout; allow losses. | Net, loss and zero-cost tests. |
| 10 | Named saved estimates | Save immutable price/cost snapshots; explicit 200-record capacity. | Estimate serialization and validation tests. |
| 11 | Reopen saved loads | Confirm replacement of current draft; retain historical quote values. | Snapshot serialization tests; device action pending. |
| 12 | Actual sale receipts | Record yard/date/payout/cash expenses/mileage and compare against original gross. | Receipt arithmetic and serialization tests. |
| 13 | Earnings summaries | Display local-month and all-recorded-sales cash net, labor adjustment, miles and modeled fuel. | Receipt math verified; month-boundary device review pending. |
| 14 | Record deletion controls | Confirm separate estimate and receipt deletion; receipts retain totals after estimate removal. | Independent receipt round-trip test; Android deletion pending. |
| 15 | Share/export | Share source-aware estimates and export all receipts through the file picker as CSV. | CSV escaping/formula-prefix tests; Android chooser/export pending. |

All existing features remain in source. No APK/AAB, Google Play submission, production AI, live pricing or automatic GPS mileage is claimed.
