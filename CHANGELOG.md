# October 29, 2026 — stop canceled photo preparation

- Pass request cancellation into JPEG preparation; stop at decode/compression checkpoints.
- Preserve the original photo and existing stale-result/upload protections.
- Add two device regressions (uncompiled/unrun); 33 Python and 32 gateway tests pass.
- Fresh Android build remains blocked at Gradle download; no new APK.

# October 28, 2026 — release rejected photo-upload connections

- Close incomplete uploads after flushing early route, format, size or quota errors.
- Add five real TCP regressions; all 32 gateway tests pass.
- Android package unchanged; live Gemini and phone acceptance remain pending.
- See UPLOAD_REFUSAL_2026-10-28.md for validation and remaining release gates.

# October 26, 2026 — fresh camera retry and photo-bounds Preview APK

- Fixed sampled photo dimension rounding for odd-sized images.
- Added odd-dimension and unreadable-photo Android regressions.
- All 173 Android/JVM tests passed; lint has zero errors and 73 warnings.
- App and instrumentation APKs compiled; Preview signature/package verified.
- All 27 gateway and seven APK-verifier tests passed.
- Camera instrumentation is compiled but not executed. Phone and live Gemini
  acceptance remain pending; production AI is not configured in this Preview.

# October 16, 2026 — fresh camera permission recovery APK

- Android build unblocked; testDebugUnitTest, lintDebug and assembleDebug pass.
- 169 Android/JVM tests pass. Lint has zero errors and 79 warnings.
- Fresh Preview APK signature/metadata verified; camera recovery code confirmed in packaged DEX.
- Live AI remains unconfigured; physical-phone acceptance and production release still pending.

# October 16, 2026 — camera permission recovery

- Retry camera startup on return from Android Settings when permission has been granted.
- Guard duplicate startup requests from lifecycle and permission callbacks; preserve photo review.
- Upload test accepts connection reset only after a complete HTTP reply; incomplete replies still fail.
- All 25 gateway tests pass. Android compilation/device acceptance pending: Gradle distribution download is blocked in this environment.

# October 14, 2026 — gateway upload deadline

- Added a 15-second total photo-upload deadline; trickling bytes cannot extend it.
- Incomplete uploads free concurrency without consuming provider budget.
- Four regression tests added; all 25 gateway tests passed. Android source unchanged.

# v4.9.6 — overall photo request deadline, October 14, 2026 (UTC)
- Add a 60-second overall deadline that disconnects stalled uploads and delayed responses.
- Reject late results even when disconnect is ignored; normalize timeout recovery messaging.
- Cancel watchdog resources after requests; preserve explicit user cancellation.
- 164 Kotlin tests passed, including 6 new deadline tests. Backend unchanged from its 21-test pass. No new APK/AAB; Android build remains blocked by the previous Gradle download failure.

# v4.9.6 — HTTP client verification, October 14, 2026 (UTC)
- Extract photo HTTP transport for direct standalone compilation and testing.
- Stop cancelled work after upload and around response reads.
- Add 9 HTTP tests, including real socket cancellation and redirect/body-limit checks.
- 158 Kotlin and 21 gateway tests passed; release guards passed. Android rebuild blocked by Gradle download; no new APK/AAB.

# v4.9.6 — verified preview rebuild, October 12, 2026
- Rebuilt the phone preview APK with the client response deadline and earlier camera fixes.
- Abort server-side AI work and release the request slot when the client disconnects; backend deployment required.
- Android tests: 145 passed. Gateway tests: 21 passed. Lint: zero errors and 78 warnings. Debug APK v2 signature verified.
- Production Gemini configuration, physical-phone acceptance and Play release remain pending.

# v4.9.6 — client response deadline, October 12, 2026
- Reject late AI response bodies even when a service repeatedly sends small chunks.
- Reduce socket read waits to the remaining response budget and show a readable timeout recovery message.
- Four new deadline regression tests; 145 core tests passed. Android SDK 36 photo-client compilation passed. Full APK rebuild remains blocked by plugin resolution.

# v4.9.0
- Retain separate quotes for each yard/grade; normalized updates and targeted deletion, 500-quote bound.
- Add explicit-consent HTTPS AI client, validated visual estimates and metadata-free resized photos.
- Add server-side Gemini gateway, daily request budget, throttling and mocked-provider regression tests.
- Restore camera review/results, clean failed/abandoned captures and ignore stale async results.
- Prepare app bundle build; production configuration and phone/Play acceptance remain pending.

# v4.8.0
- Add current-load yard comparisons using yard-specific saved quote snapshots.
- Per-yard fuel, mileage, travel labor and fees; estimated net ranking, net gap and break-even payout.
- Missing-grade exclusion, quote-age/date review, shareable grade-level provenance.
- Restore comparison inputs and planner navigation after activity recreation.
- Add 15 regression tests; 123 total core tests. No ledger schema change.

# v4.7.0 — October 3, 2026
- Implemented the 50 source capabilities listed in MILESTONES_4.7.0.md.
- Added earnings reports and filtered CSV export; profit targets/scenarios; current-load operations; estimate rename/copy and receipt corrections.
- Added conflict-safe, capacity-checked backup merging, pre-import recovery, and deletion undo.
- Export bytes now live in temporary cache files; saved state retains tokens to avoid large Bundle payloads.
- Added 50 pure-model regression tests; 108 core tests passed.
- Android build/test/lint and minified unsigned release compilation passed; debug test APK and Gradle wrapper supplied. Fixed JVM targets, nullable screen overrides and backup-rule XML.
- Retained v1 portable ledger compatibility. Production AI, live prices and Play release tasks remain pending.

# v4.6.0 — October 2, 2026
- Portable JSON ledger export and validated restore with record-count preview and confirmation.
- Preserves captured prices, trip costs and independent sale receipts.
- 58 JVM tests passed; Android build and device verification pending.

# v4.5.0
- Implemented 15 connected mixed-load, trip-planning, saved-estimate and sale-ledger source milestones.
- Captured per-item prices with provenance; lb/kg entry and weight editing.
- Persisted working draft; round-trip mileage/fuel, labor, fees, net and break-even calculations.
- Named estimate snapshots, reopening and sharing; actual payout, expenses, sale date and mileage records.
- Monthly/all-sales earnings and mileage summaries; confirmation before deletions; CSV file-picker export with safe field quoting.
- 23 new model/serialization tests; full 45-test JVM suite passed. Android build/lint/device verification pending.
- versionCode 46. Source package only; AI, billing and live-price services remain pending.

# v4.4.0
- Added validated user-entered scrapyard quotes per metal, with contact-name selection.
- Load and motor estimates use saved quotes, with explicit fallback to demo benchmarks.
- Show quote source, entry date and 24-hour confirmation reminders.
- Source-aware price snapshots, preserving legacy benchmark history during migration.
- Added seven quote regression tests; execution pending Android/JVM toolchain.
- versionCode 45. No AI, billing or live-price service is activated by this update.

# v4.3.0
- Daily opening-screen Bible verse: 31 offline KJV passages, stable per local date, refreshes on resume and while open.
- Weather card: city selection, conditions icon, Fahrenheit temperature, model timestamp, manual refresh and explicitly stale offline report.
- In-app NOAA/NWS radar WebView, centered on saved city, animated radar and zoom controls provided by NWS.
- Manual real scrapyard contacts, dialer and directions; fictional demo-yard dialing removed.
- Validated calculator inputs and separated motor calculation model.
- Added daily date-boundary and motor-economics regression tests.
- Updated privacy and release status; no APK/AAB or completed Play publication claimed.

## 4.9.3 — demo-data safety hardening
- Replaced realistic-looking fictional scrapyard names, phone numbers, URLs and addresses with unmistakable DEMO labels.
- Demo yards no longer carry callable phone numbers or example.com links.
- Updated in-app visible version to 4.9.3 and Android versionCode to 52.
- Functional behavior remains unchanged; real local businesses belong in My Scrapyard Contacts until a verified local-yard provider is integrated.

## 4.9.5 release-candidate hardening (2026-10-06)
- Corrected BUILD_AND_SIGN.md from stale v4.9.0/versionCode 50 instructions to current v4.9.5/versionCode 54.
- Extended release-consistency automation to cover BUILD_AND_SIGN.md and require its fresh-verification warning.
- Re-ran release configuration and consistency gates successfully.
- Re-ran AI gateway tests: 12/12 passing.
- Added FINAL_RELEASE_CANDIDATE_4.9.5.md with explicit verified vs external release gates.

## 4.9.5 — Play Store preparation
- Aligned Play submission docs and listing copy to v4.9.5 / versionCode 54 / API 36.
- Added Data Safety review worksheet and Play Console submission runbook.
- Updated privacy publication draft for the production AI consent flow.
- Removed stale v4.9.0 unsigned AAB from the release handoff.
- Extended release consistency checks to cover Play submission materials.
