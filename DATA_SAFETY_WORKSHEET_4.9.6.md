# Scrappy v4.9.6 — Google Play Data Safety worksheet

This is a preparation worksheet, not a submitted Play Console declaration. The developer must verify the deployed production services/SDKs and answer the Play form truthfully at submission time.

## Data flows visible in the current source

### Camera photo / AI identification
- User takes a photo in the camera workflow.
- Photo is cached locally during the workflow.
- Only after the user chooses **Send and analyze**, a resized/re-encoded JPEG is sent to the configured Scrappy AI gateway and then processed through Google Gemini by that server.
- Original EXIF metadata is not intentionally forwarded by the app's re-encoding path.
- The Android app contains no Gemini API key.
- Production hosting/proxy logs and Google's handling must be reviewed before the final Play declaration.

**Play form review:** determine the applicable Photos/Videos data type and whether the production processing qualifies as collection under Google's current definitions; document purpose as app functionality. Verify retention, deletion, encryption in transit, and sharing/service-provider treatment from the deployed service configuration.

### Weather
- City search text is sent to Open-Meteo when the user searches.
- Selected city latitude/longitude are sent to Open-Meteo for weather.
- Scrappy does not request Android GPS location permission.

**Play form review:** assess whether the selected-city coordinates constitute approximate location under the current Play definitions and declare consistently with the production behavior.

### Radar
- The embedded NOAA/NWS radar site receives normal web-request information and the selected map location when opened.
- WebView/site storage may be used by the external site.

### Local app records
Favorites, manual scrapyard contacts, settings, chosen weather city/report, yard quotes, price snapshots, mixed loads, trip-cost inputs, estimates, and sale records are stored locally by Scrappy. Android app backup is disabled. User-directed export/share can copy selected records outside Scrappy to a destination/app the user chooses.

### External actions
Calling opens the dialer. Directions and nearby-yard searches open an external map app. Data handling after that handoff is controlled by the selected external app.

## Before completing Play Console Data safety
1. Deploy the exact production AI gateway and document host/proxy logging and retention.
2. Review the current Google Gemini terms/data handling for the account/product actually used.
3. Review current Open-Meteo and NOAA/NWS behavior/terms.
4. Check the final dependency tree for any added analytics, crash reporting, ads, or other SDK collection. None should be declared absent merely because it is absent from this source snapshot if the production build adds it.
5. Make the public privacy policy match the final deployed behavior exactly.
6. Complete every Play Console Data safety question; Google requires the form for closed/open/production tracks.

Current version: 4.9.7 — dark ScrapRight-style UI redesign (Prices / Scan / Yards / More tabs, real Covington yards, Georgia index prices).
