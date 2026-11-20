# Scrappy build and photo review hardening — October 8, 2026

## Changes

- Updated Android Gradle Plugin from 8.7.3 to 8.9.2 for compile/target SDK 36. Google's current compatibility table lists 8.9.1 as the minimum for API 36; 8.9.2 includes subsequent patch fixes.
- Updated the Gradle distribution to 8.11.1 and pinned its official SHA-256 checksum. The existing wrapper launcher JAR remains unchanged and requests the new distribution through its properties; a full wrapper regeneration has not been performed locally.
- GitLab now installs SDK Build Tools 35.0.0, matching AGP 8.9's documented default/minimum, and retains lint reports as downloadable artifacts.
- README and build instructions now refer to Gradle 8.11.1.
- Local camera review and AI upload share a bounded bitmap decoder with EXIF orientation handling. Photo preview decoding runs on the worker instead of loading the full captured image through ImageView on the main thread.
- Preview results are discarded if the activity closes or a different photo becomes active. Stale results cannot replace the current photo, and discarded bitmaps are recycled.

## Verification and limits

Release configuration and release/document consistency checks pass. Pipeline YAML parses and embedded shell commands pass Bash syntax checks. A fresh wrapper attempt requests Gradle 8.11.1 but fails before Gradle starts with Network is unreachable. SDK is unavailable locally. Android code compilation, lint, runtime behavior and APK generation remain pending. The remote GitLab pipeline has not been run from this session.

No backend code changed; earlier backend verification was 12/12 passing. No new Android test pass is claimed. The source remains version 4.9.6 / code 55 and includes the earlier camera-permission, retake, weather-refresh and radar-error fixes.

## Next device checks

After GitLab successfully builds the APK, test high-resolution photos, portrait/landscape orientation, restored photo review, rapid retake, immediate AI analysis after capture, permission denial, offline radar and repeated weather refresh. Production AI still needs a deployed gateway and configured public privacy URL.

## Sources

- Android API/AGP and Gradle compatibility: https://developer.android.com/build/releases/about-agp
- AGP 8.9 build tools, JDK requirements and patch notes: https://developer.android.com/build/releases/agp-8-9-0-release-notes
- Official Gradle 8.11.1 distribution checksum: https://gradle.org/release-checksums/
