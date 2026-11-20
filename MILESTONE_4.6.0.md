# v4.6.0 — Portable ledger backup and restore

Implemented source milestone: export the current load, saved estimates and sale receipts to a versioned JSON file; import a file, validate it, preview the incoming/current record counts, and confirm replacement. All three ledger keys are written with one SharedPreferences commit after validation. Sales whose estimates were previously deleted remain valid.

Open Mixed Loads, Trips & Sales → Backup & restore ledger. Export first, save the file somewhere recoverable, and choose it to restore on another phone. Canceling either picker makes no ledger change; canceling the replacement dialog makes no ledger change. Export captures the saved ledger when the destination is selected. Unapplied edits are excluded. Imported historical quote values and mileage remain unchanged.

Files are limited to 8 MiB. Unsupported format/version, corrupt JSON, invalid financial values, duplicate IDs and excess record counts are rejected. Capacity remains 100 current-load items, 200 estimates and 200 sales. This is replacement, not merging. Backups are unencrypted and contain financial information and yard names; protect them. Photos, scrapyard contacts, price quotes, settings and credentials are excluded.

Verification: 58 JVM tests passed, including 13 new backup tests. Android compilation, persistence/file-picker behavior and device checks remain pending because Android SDK and Gradle are unavailable here. No installable APK/AAB is included. Production AI, live pricing and Google Play release blockers remain as documented in the prior project.

Device acceptance checks: export/import populated and empty ledgers; verify all prices, dates, receipts and mileage; cancel pickers and replacement confirmation; reject damaged/oversized backups without record changes; restore receipts after deleting their estimate; rotate during a picker; simulate inaccessible destination/source and low storage. Test Android 26 and 36. A file-picker or storage error must be shown to the user.
