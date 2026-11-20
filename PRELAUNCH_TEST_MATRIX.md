# Pre-launch test matrix

- Fresh install: launches to Scrappy home without crash.
- Camera denied: app explains permission requirement and remains usable.
- Camera granted: preview opens and photo capture succeeds on a physical device.
- Price guide: all metal rows open/save without crash.
- Motor calculator: blank/zero/normal values do not crash; totals are sensible.
- Load calculator: estimate and Android share sheet work.
- Yard comparison: sorting, favorite toggle, dialer, and directions intents work.
- Persistence: favorites, labor rate, default metal, and price history survive restart.
- Offline: reference/calculator features remain usable without network.
- Privacy: no Gemini/API secret is packaged in the client.
- Release: minification build launches and core flows work.
- Accessibility: large text, TalkBack labels, contrast, and touch targets receive manual review.

## v4.4.0 yard quote checks (pending)
- Save Copper #2 at $4/lb, then estimate a 10-lb load: $40 with yard and date visible.
- Confirm motor separated value uses that copper quote and labels any demo steel fallback.
- Enter Steel at $0/lb: use zero, without reverting to demo.
- Update a quote, restart the app and verify the latest quote persists.
- Remove a quote: restore demo with its label; retain prior snapshots and their source.
- Advance device time to exactly 24 hours after entry: display confirmation reminder.
- Reject blank yard, invalid price and out-of-range price.
- Upgrade with existing benchmark history: old entries remain after saving a new source-aware snapshot.
- Yard names containing pipes or quotes survive snapshot save/restart.

## v4.5.0 mixed-load/trip/sale checks (all device checks pending)
- Home → Mixed Loads, Trips & Sales opens; TalkBack and large text remain usable.
- Add 10 lb Copper #1 with a saved $4 quote and 100 lb demo Steel: gross $49, total 110 lb.
- Apply 40 round-trip miles, 20 MPG, $3 gas, 60 travel minutes at $20/hour, $2 fees: fuel $6, labor $20, net $21, break-even $28.
- Add 1 kg at $4/lb: approximately 2.20462262 lb and $8.82 value.
- Change a yard quote after adding an item: existing item price remains; removing/re-adding captures latest price.
- Edit item weight; cancel/remove/clear confirmations retain data when canceled.
- Invalid/blank/negative/nonfinite values show feedback; zero MPG and empty estimates are rejected.
- Added rows/applied costs survive navigation, restart and process death; unapplied form edits are explicitly unsaved.
- Save named estimate, reopen with confirmation, share source/date-aware report.
- Record $55 payout, $8 actual expenses, 40 actual miles against the example: cash net $47, labor-adjusted $27, payout difference +$6, modeled fuel $6.
- Record actual 60 miles against the same estimate: modeled fuel $9; entered fuel expenses are not double-deducted.
- Switch sale date to prior month; verify current-month totals use device local date and all-sales totals include both months.
- Multiple receipts count independently; receipt names/dates/mileage survive restart.
- Delete an estimate: its sale records and recorded economics remain. Cancel each deletion type and verify no removal.
- Export CSV with comma/quote/newline/formula-prefix yard names; import into a spreadsheet and verify fields, values and miles.
- Cancel file picker; retry export; rotate/recreate while picker open; test write failure, unavailable share app, insufficient storage.
- Test 100-item/200-estimate/200-sale limits: new writes are rejected with feedback rather than dropping oldest entries.
- With malformed ledger JSON: display read error, do not erase stored bytes; home remains accessible.
- Run Gradle testDebugUnitTest, assembleDebug and lintDebug, then release minification build and physical-device checks.

## v4.7.0 acceptance checks (all phone checks pending)
- Home and ledger open Earnings & Profit Tools; returning from load tools refreshes the ledger draft.
- Filter reports by date, yard and text; Apply and Reset work. Test local midnight, DST and Monday/year boundaries. Sort all four ways; verify empty/zero-mile summaries show Unavailable.
- Compare report totals with saved receipts; verify modeled fuel is not deducted twice and multiple receipts count independently.
- Export matching rows; cancel picker, rotate, kill/recreate process with picker open, use non-ASCII/formula-prefix names, deny destination access and simulate full disk. Cached bytes are not placed in Bundle. Lost cache presents retryable error.
- What-if previews do not modify the ledger. Verify price/fuel/distance changes, target weight/price/distance, and over-capacity warnings. Negative/nonfinite/out-of-range inputs are rejected.
- Rename/copy estimates; copies retain prices and costs but receive fresh IDs. At 200 estimates, copy is rejected without loss.
- Correct a receipt without adding another; preserve original gross/labor and date/ID; verify proportional modeled fuel and zero-original-mile handling. Reject future dates. Cancel confirmation with no changes.
- Delete estimate/receipt, undo once, and verify undo is unavailable after another ledger change. Test restart and storage failure. Undo does not remove unrelated receipts.
- Scale/copy/combine/sort/reprice/reset load: review confirmations, cancel without changes, preserve historical estimates/sales, reject absent quotes and full load, and verify quote provenance. Toggle lb/kg repeatedly without changing physical quantity.
- Import old v4.6.0 backups using all three modes. Preview add/skip counts; identical repeated import adds nothing; conflict or excess capacity changes nothing. Reject corrupt JSON/malformed UTF-8/oversized files.
- After merge/replacement, restart and recover pre-import ledger; warn about later edits. Recover again to reverse; export portable backup first. Cancel all dialogs/pickers with no changes.
- Share current load, receipt, filtered earnings and ledger summary; verify source/date/privacy notices and chooser failure handling.
- Verify TalkBack, 200% font size, scrolling with 200 receipts, rotation, API 26/API 36, no-network operation, upgrade from v4.6.0 without loss, and minified release build behavior.

## v4.8.0 yard comparison — pending device checks
- Empty load and no saved quotes show an actionable message.
- Add two yards with different distances and grade quotes; verify estimated net ordering.
- Missing grade stays unranked; zero quote remains a valid loss-making option.
- Update/remove/clear comparison; verify ledger draft, estimates and receipts unchanged.
- Rotate or trigger process recreation: added yards and entered fields restored; planner reopens correctly.
- Share chooser sends the displayed snapshot; cancel/back remains usable.
- Large font and screen reader labels; small display scroll; 20-yard limit reports a clear error.

## v4.9.0 — pending device/provider checks
- Add the same metal at two yards; both remain stored. Update/remove one; the other remains.
- Existing v4.7 quote, ledger and settings data remain readable.
- Unconfigured AI never uploads; manual helper stays reachable.
- Configured HTTPS service and published policy: decline consent, then approve; verify one upload only.
- Verify image rotation and omission of EXIF/location metadata in the transmitted JPEG.
- Verify valid/Unknown/refused/malformed responses and offline/timeout/429/503 behavior.
- Retake/back/recreate during request; obsolete responses ignored; no automatic resend after recreation.
- Photo/result restoration and cache cleanup; long AI results scroll without hiding controls.
- Run real deployed-provider requests and check hosting retention, logs and durable quotas.
