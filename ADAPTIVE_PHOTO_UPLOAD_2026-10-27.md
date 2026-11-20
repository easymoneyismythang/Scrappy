# Adaptive photo upload checkpoint — October 27, 2026 (New York)

Scrappy remains version 4.9.6 (55). A fresh debug-signed Preview APK and a
compiled Android instrumentation APK were produced from this checkpoint.

## Photo upload fix

A detailed 1600-pixel photo can exceed the service's 1 MiB limit at JPEG
quality 80. Previously the app rejected it and asked for a retake, even though
the photo was readable. The upload encoder now tries qualities 80, 65 and 50;
if still too large, it reduces the upload copy's dimensions to three quarters
and repeats. The loop terminates with a valid bounded JPEG or an explicit
preparation failure. The original cached photo is not rewritten. All paths
re-encode pixels, so original EXIF metadata is not included in the upload.
Visible content in the photo remains visible to the service.

Two new Android regressions cover a detailed photo that exceeds the old
limit, preservation of the original, metadata removal after adaptive encoding,
a decodable bounded output, and unchanged small-photo dimensions. There are
14 instrumentation tests total (seven camera flow, seven photo tests).

## Executed verification

- Gradle testDebugUnitTest, lintDebug, assembleDebug and assembleDebugAndroidTest succeeded.
- 154 Android unit tests and 19 JVM transport tests passed, zero failures.
- The standalone core runner also passed the same 173 cases; these are not additional distinct tests.
- 27 gateway tests passed with controlled provider behavior.
- 12 APK-verifier tests passed; release consistency and configuration checks passed.
- Lint: zero errors/fatal issues, 77 warnings. Warnings remain for review.
- Real SDK apksigner and aapt verified the new APK signature, package and version.
- APK DEX contains the adaptive encoder path; test APK DEX contains the detailed-photo regression.
- ZIP integrity verified. See verification/adaptive-photo-upload-2026-10-27.

## Phone installation and remaining gates

The new debug certificate differs from the October 26 checkpoint. An ordinary
update over that exact build will fail. Preserve the installed app and its
data; do not uninstall or clear storage to force installation. A compatible
update requires the original signing key. This ZIP contains no signing keys.

No Android phone or emulator is connected here. The 14 instrumentation tests
compiled but were not executed. Camera capture, retake, permission recovery,
rotation, backgrounding, photo encoding and installation need device acceptance.

AI endpoint and privacy URL are empty in this Preview. No Gemini credential or
model is configured in this environment. The manual helper and local camera
flow are available, but live Gemini is not enabled or accepted. Completion still
requires the deployed authenticated/abuse-controlled service, provider and
privacy configuration, live acceptance, final store assets/contact details,
protected production signing, and Play Console testing/submission. Live pricing
and yard-service claims also require verified data sources. No production AAB
or Play submission is claimed.

GitHub was checked read-only: Scrappy420 and Scrappy are public; Scrappy- returns
404. No current code was uploaded publicly, preserving the earlier preference
for a private project repository.
