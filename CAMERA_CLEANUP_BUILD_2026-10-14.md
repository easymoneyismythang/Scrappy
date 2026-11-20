# Scrappy v4.9.6 camera cleanup and Android build

October 14, 2026 UTC.

## Changes
Connection cleanup exceptions no longer escape explicit cancellation or replace a successful AI result or the original request error. Cancellation is checked before opening the response stream; expired overall requests are checked there too. Five regression tests cover throwing disconnect callbacks, late cancellation attachment, successful response cleanup, original service failure, and cancellation after headers.

The real loopback HTTP tests use the JDK HTTP-server module, which the Android test compiler cannot resolve. They now live in a small Kotlin JVM module, transport-tests, compiling the same six production transport sources. testDebugUnitTest depends on its test task, so the standard Android verification keeps all transport regressions. The standalone runner and GitLab JUnit collection include the new test location. No server library was added to the APK.

## Executed verification
- Standalone Kotlin: 169 tests passed.
- Gradle testDebugUnitTest, lintDebug, assembleDebug: BUILD SUCCESSFUL.
- Android unit suite: 154 passed; JVM transport suite: 15 passed. No failures/errors/skips.
- Gateway: 21 mocked-provider tests passed.
- Release configuration and handoff consistency guards passed.
- Lint: zero errors/fatal issues, 79 warnings. Existing warning categories remain for later review.
- Preview APK signature verified. Package com.scrappy.app.preview, versionName 4.9.6/versionCode 55, minSdk 26/targetSdk 36.

The Gradle download and Java compiler blockers were resolved using checksum-verified Gradle 8.11.1 and a complete Java 17 JDK. System certificate trust was used for the build connection; no trust bypass was added to the app or project.

## Deliverables and remaining work
Scrappy-v4.9.6-camera-cleanup-preview.apk is a debug phone-testing app. Scrappy-v4.9.6-camera-cleanup-source.zip contains current source and verification evidence. It contains no production release or signing key.

The Preview's AI endpoint and privacy URL are empty; camera capture/manual helpers can be tested, but live Gemini identification remains disabled. Physical phone behavior, Android socket cancellation, deployed gateway/provider behavior, production hosting/privacy configuration, release signing and Play acceptance remain unverified. Existing REQUEST_DEADLINE_2026-10-14.md and NEXT_ACCEPTANCE_MILESTONE.md contain the phone test criteria. A successful JVM socket test does not establish device behavior.
