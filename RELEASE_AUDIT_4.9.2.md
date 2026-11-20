# Scrappy v4.9.2 — release-blocker audit

Date: 2026-10-06

## Verified in source
- Android permissions are limited to CAMERA and INTERNET; GPS/location permission is not requested.
- Cleartext HTTP is disabled.
- Android cloud backup and device-transfer extraction are disabled for local Scrappy data.
- No Gemini API secret is embedded in Android. AI accepts only configured HTTPS endpoint/privacy URLs.
- AI photo upload requires consent for every request and re-encodes pixels before upload, excluding original EXIF/location metadata.
- Weather uses Open-Meteo over HTTPS without GPS permission. Radar uses NOAA/NWS over HTTPS.
- Built-in yard payout comparison is explicitly labeled demo data, not live local listings.

## Verification status
- AI gateway Node suite rerun on 2026-10-06: PASS, 12/12.
- Core/data suite: previous packaged v4.9.0 verification records PASS. Fresh rerun in this workspace was NOT completed because the offline cached Kotlin compiler dependency failed checksum verification; this is recorded as a tooling/dependency-cache blocker, not an app test result.
- Existing packaged Android lint: 0 errors / 54 warnings, primarily hard-coded UI/localization warnings.

## Still blocking production release
1. Fresh network-enabled Android build and installable Preview APK.
2. Physical-device acceptance matrix: camera, permission denial/retry, capture/retake, rotation/backgrounding, persistence, weather/radar, ledger import/export, dialer/maps intents.
3. Production HTTPS Gemini gateway with secret management, durable quotas, logging/retention policy, and public privacy policy.
4. Real Gemini tests: metal/non-metal/ambiguous images, refusal, timeout, 429, 503, rotation and cancellation behavior.
5. Keep demo yard data clearly labeled or replace it with a legitimate live/local data source before representing it as current/local.
6. Functional bugs first; cosmetics/accessibility/localization pass follows acceptance testing.
7. Final signed Play App Bundle plus Play Console declarations/listing review.

## Version checkpoint
Android versionName: 4.9.2
Android versionCode: 51
