# Scrappy v4.9.4 — resilience checkpoint

## Source fixes
- Prevent the Camera privacy-policy action from crashing when Android has no activity/browser able to open HTTPS links.
- Correct the in-app Privacy & Safety version label to v4.9.4.
- Synchronize the home-screen version to v4.9.4.
- Advance Android package metadata to versionCode 53 / versionName 4.9.4.

## Release status
Physical-device acceptance remains required before Play release. In particular: camera lifecycle/permission behavior, configured live AI endpoint, weather/radar, persistence, export/import, and install/update behavior.

## Verification limitation
This checkpoint does not claim a fresh Gradle/JUnit pass: the current isolated build environment cannot obtain the required Gradle distribution and the cached standalone Kotlin compiler dependency previously failed checksum verification. The independent backend suite can still be run separately.
