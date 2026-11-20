# Scrappy v4.9.3 release audit

This checkpoint hardens demo-data presentation before physical-device acceptance testing.

## Fixed
- Fictional yard records can no longer look like real businesses: names are prefixed `DEMO`, addresses say `Fictional demo only`, and phone/URL fields are blank.
- Home screen version now matches the package version (4.9.3).
- Android package version advanced to versionCode 52 / versionName 4.9.3.

## Still blocked / required
- Build a fresh Preview APK on an Android/Gradle-capable machine with network access.
- Run the physical-device acceptance matrix, especially camera capture/import, Gemini gateway behavior, lifecycle/background/rotation, weather/radar, persistence/export/import, and external intents.
- Replace the demo comparison experience with verified local-yard discovery before representing it as live/local data.
- Complete visual redesign only after functional acceptance is clean.

No claim of a fresh full Android unit-test pass is made by this audit because the offline Kotlin compiler cache previously failed checksum verification. The AI gateway suite was independently verified in the prior checkpoint.
