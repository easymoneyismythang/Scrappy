# Scrappy v4.9.6 — Play Console submission runbook

1. Publish `PRIVACY_POLICY_DRAFT.md` after replacing all bracketed publisher/hosting details; use that HTTPS URL as `scrappyAiPrivacyUrl`.
2. Deploy the AI gateway and obtain its HTTPS `/v1/identify` URL. Keep `GEMINI_API_KEY` only on the server.
3. Build and verify on a networked Android development environment:
   - `./gradlew clean testDebugUnitTest lintDebug`
   - `./gradlew bundleRelease -PscrappyAiEndpoint=https://YOUR_HOST/v1/identify -PscrappyAiPrivacyUrl=https://YOUR_HOST/privacy`
4. Sign with the protected upload key / Play App Signing workflow. Do not commit the keystore or passwords.
5. Test the matching build on a real Android phone and complete `PRELAUNCH_TEST_MATRIX.md` / `NEXT_ACCEPTANCE_MILESTONE.md`.
6. In Play Console create/select Scrappy (`com.scrappy.app`) and complete Store listing using `STORE_LISTING_DRAFT.md` plus final graphics/screenshots.
7. Complete App content forms, using `DATA_SAFETY_WORKSHEET_4.9.6.md` as a source-review aid, not as an automatic answer key. Complete privacy policy, ads, app access, target audience/content, content rating, Data safety, and every additional form Play presents.
8. Upload the freshly built v4.9.6 AAB to Internal testing first. Resolve Play automated/pre-launch findings.
9. Move to Closed testing when ready. If the developer account is a personal account created after November 13, 2023, current Play rules require at least 12 testers continuously opted in for 14 days before applying for Production access.
10. Apply for Production access if required, resolve review feedback, then create the Production release.

As of October 2026, Android phone submissions must target API 36; this project is configured for API 36. Re-check current Play policy on submission day.

Current version: 4.9.7 — dark ScrapRight-style UI redesign (Prices / Scan / Yards / More tabs, real Covington yards, Georgia index prices).
