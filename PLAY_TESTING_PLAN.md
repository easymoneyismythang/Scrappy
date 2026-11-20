> Current version: v4.5.0 / versionCode 46. 45 core JVM tests passed; Android build, lint and device verification remain pending. See README.md and IMPLEMENTATION_STATUS.md. Earlier version-specific statements below are historical planning.

# Scrappy Play testing plan — v4.1.0

## Internal test
1. Build a signed release Android App Bundle (`app-release.aab`).
2. Create Scrappy in Play Console using the permanent package name `com.scrappy.app` only if that package name is confirmed as final.
3. Enable Play App Signing and upload the AAB to Internal testing.
4. Test install/update, camera permission/capture, every calculator, favorites/settings persistence, dialer/directions intents, rotation/relaunch, and offline behavior.

## Closed test
For a personal Play developer account created after November 13, 2023, Google currently requires at least 12 testers opted in continuously for 14 days before applying for production access. Confirm the requirement shown in the account's Play Console because account eligibility can differ.

Collect tester feedback on crashes, confusing metal guidance, incorrect calculations, camera failures, accessibility, and device compatibility. Fix release-blocking issues and increment versionCode for every uploaded replacement bundle.

## Production gate
Do not submit to production until the store listing, privacy-policy URL, Data safety answers, content rating, target audience, app access declarations, screenshots/icon/feature graphic, support email, countries/pricing, and production-access requirements are complete.
