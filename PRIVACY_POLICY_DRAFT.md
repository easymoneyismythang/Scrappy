# Scrappy Privacy Policy — publication draft for v4.9.5

**Effective date:** [ADD DATE]  
**Developer:** [ADD LEGAL DEVELOPER NAME]  
**Contact:** [ADD SUPPORT EMAIL]

Scrappy is a scrap-metal identification and estimating app. This policy describes the information handled by the v4.9.5 release candidate. Replace the bracketed publisher details and verify the final production service configuration before publishing this policy.

## Information stored on your device
Scrappy stores app settings, favorites, manual scrapyard contacts, your selected weather city and recent weather information, user-entered yard quotes and price snapshots, mixed-load information, trip-cost inputs, saved estimates, and actual sale records on your device. Ledger recovery/undo data may also be stored locally. Android app backup is disabled.

Camera photos are kept in temporary app storage for the camera workflow. Closing the camera removes its current photo, and the app includes cleanup for abandoned temporary photos. Information you deliberately export or share is sent to the file location, cloud document provider, or other app you choose and is then governed by that destination.

## AI metal identification
Scrappy's production release is configured with a Scrappy-operated HTTPS AI gateway. A camera image is not sent for AI analysis merely by taking the photo. Before each AI upload, Scrappy presents a consent action. If you choose **Send and analyze**, the app creates a resized and re-encoded JPEG copy and sends it to the configured Scrappy service for processing with Google Gemini. Original EXIF metadata is not intentionally included in that re-encoded copy, although information visibly present in the image remains visible.

The Android app does not contain the Gemini API key. The supplied Scrappy gateway processes image/result data in memory and maintains request-budget information; production hosting infrastructure may also create security or operational logs. The developer must document the final host's logging/retention here before publication and ensure the deployed service and Google AI configuration comply with applicable terms and Google Play User Data requirements.

AI identification is a visual estimate and is not laboratory analysis, guaranteed alloy identification, or a scrapyard grade decision.

## Weather and radar
When you search for a weather city, the search text is sent to Open-Meteo. Weather requests send the latitude and longitude of the city you selected. Scrappy does not request Android GPS location permission.

Opening radar loads the U.S. National Weather Service radar website inside the app. That service receives normal web-request information and the selected map location and may use website storage.

## Calls, directions, and nearby-yard searches
Calling opens your phone dialer. Directions and nearby-yard searches open an external map application. Those applications and services control their own data handling after the handoff.

## Price and financial estimates
User-entered metal prices, yard names, quote times, price snapshots, load estimates, trip costs, and sale records are stored locally unless you choose to export or share them. Scrappy prices and calculations are estimates and are not guaranteed offers or payouts.

## Security and retention
Scrappy requires HTTPS for its configured AI service and disables cleartext network traffic at the Android application level. Local data remains until you edit/delete it or clear the app's storage, subject to temporary recovery/undo copies described by the app. Exported/shared copies remain wherever you chose to save or send them.

## Third-party services
The production app may communicate with Google Gemini through the Scrappy AI gateway, Open-Meteo for weather, and NOAA/National Weather Service for radar. Review and link the applicable current third-party privacy terms on the published policy page before release.

## Changes
If Scrappy adds analytics, advertising, accounts, new cloud storage, new AI processing, or other data collection, this policy and the Google Play Data safety declaration must be updated before or when those changes are released.

## Contact
Questions about privacy can be sent to: **[ADD SUPPORT EMAIL]**.
