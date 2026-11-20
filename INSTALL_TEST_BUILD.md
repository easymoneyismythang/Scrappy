> For new camera testing, use **Scrappy-v4.9.6-camera-test.apk**, which installs as **Scrappy Camera Test** beside your existing apps. This avoids replacing the differently signed Preview and leaves its local data in place. Follow CAMERA_TEST_INSTALL.md. The ordinary Preview instructions below remain relevant when a compatible signing key is available.

> October 27 installation note: this fresh Preview uses a different debug signing certificate from the October 26 build. It cannot update that build with an ordinary install. Keep any existing app/data if Android rejects it; do not uninstall or clear storage to force the update. The original signing key is needed for a compatible update. AI uploads remain unconfigured. See ADAPTIVE_PHOTO_UPLOAD_2026-10-27.md.

# Install Scrappy Preview v4.9.6

A debug APK built from the Preview variant installs as **Scrappy Preview**, package `com.scrappy.app.preview`, versionName `4.9.6` / versionCode `55`. **The latest compiled APK is supplied separately as Scrappy-v4.9.6-adaptive-photo-upload-preview.apk (October 27 checkpoint). This ZIP contains source and build evidence; this build includes adaptive photo upload encoding.** It can coexist with production-package test builds (`com.scrappy.app`) because Preview uses the `.preview` application ID suffix.

On your Android phone, download the supplied APK, tap it, and allow installation from the downloading app if Android asks. Open **Scrappy Preview** after installation. To rebuild, use Java 17/SDK 36 and `./gradlew testDebugUnitTest lintDebug assembleDebug`; the output is `app/build/outputs/apk/debug/app-debug.apk`. Open Scrappy Preview and run the checks in `NEXT_ACCEPTANCE_MILESTONE.md`. AI remains disabled unless both a valid HTTPS service endpoint and public privacy-policy URL are configured; the manual helper remains available.

The preview uses separate local storage. Preserve any older test data before uninstalling or clearing prior builds. Portable ledger backups do not necessarily include every app data type, so review imports and preserve anything important separately.

This is a debug test app, not the production Play release. The production app retains package `com.scrappy.app`; final release signing requires the developer's protected upload key. No signing keys are distributed in this project.

If Android refuses to update an existing Scrappy Preview because the debug signing key differs, keep the existing app and its data. Export a portable ledger backup and preserve other important data. A compatible update needs the original signing key; uninstalling removes local data and is not a routine update step.

## Installation verification added October 21, 2026

The phone-build workflow now verifies the newly built APK with Android SDK `apksigner`, reads its package/version/minimum SDK with `aapt`, and saves its SHA-256 and signing certificate fingerprints to `verification/phone-apk.json` in the Android test-reports artifact. APK upload only proceeds after that check succeeds. A valid signature does not guarantee installation or camera/AI behavior on a physical phone.

To compare a new APK with the exact older APK installed on a phone, run:

```bash
python3 scripts/verify_phone_apk.py new-preview.apk --compare installed-preview.apk \
  --aapt /path/to/android-sdk/build-tools/35.0.0/aapt \
  --apksigner /path/to/android-sdk/build-tools/35.0.0/apksigner \
  --output verification/phone-apk.json
```

This read-only check reports mismatched package names, changed signing certificates, or a lower version code. It never installs or uninstalls anything. Separately generated debug keys can differ between build environments; the workflow does not establish a persistent signing key. Keep the original debug key protected if future builds must update an existing Preview installation without removing data.

The previously supplied `Scrappy-v4.9.6-permission-recovery-preview-3.apk` was inspected: ZIP integrity passed, ARM64 libraries are present, and an APK signing block exists. Its signature was not cryptographically verified in this environment because Android SDK tools are unavailable. The cause of the reported installation failure remains unconfirmed; obtain the exact Android error before recommending a destructive workaround. No replacement APK was built in this pass.


## Optional Android compatibility check — October 27, 2026

Supply the phone's Android **API level**, not its Android marketing version:

```bash
python3 scripts/verify_phone_apk.py new-preview.apk --device-sdk 26 --output verification/phone-apk.json
```

Use the same SDK tool path options shown above when the tools are not on PATH.
API 26 is Android 8.0, the minimum currently required by this preview. Obtain
the actual phone API level before supplying this option. You can combine it
with `--compare` to check device and update requirements in one report.
An unsupported API level produces `device_findings` and exit status 1. A
supported level means only that the minimum-SDK requirement passes; installation,
camera behavior and live AI still need separate acceptance. Omitting the option
performs the existing APK checks without claiming device compatibility.
