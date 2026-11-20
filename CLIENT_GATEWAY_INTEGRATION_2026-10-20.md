# Scrappy v4.9.6 — client/gateway integration checkpoint

October 20, 2026 UTC (October 19 evening in New York). VersionCode remains 55; this is a source/testing checkpoint, not a new Android release.

## Changes
- HTTP 408 upload failures now explain that the photo upload took too long and suggest a stronger connection or the manual helper. Error response bodies remain unread and are never displayed.
- Four host JVM integration tests connect the shared production HTTP client to the actual Node gateway: successful validated result, invalid provider result followed by recovery, user cancellation aborting provider work followed by retry, and client deadline aborting provider work followed by retry.
- The test fixture binds loopback only, uses synthetic JPEG marker bytes and a controlled provider, requires no credentials, and persists no photo. It verifies transport wiring, not JPEG decoding, Gemini accuracy, Android lifecycle, TLS hosting or real-device networking.
- Gradle transport tests declare the fixture and gateway source as inputs. Both CI build definitions provide Node 22. GitLab builds use Node 22 Bookworm with Java 17 installed; these CI definitions have not been executed remotely in this session.

## Executed verification
- `python3 scripts/run_core_tests.py`: 173 passed (154 pure Kotlin app tests + 19 host transport/integration tests).
- `node --test backend/server.test.mjs`: 25 passed.
- Release configuration and version consistency checks passed.
- `testDebugUnitTest lintDebug assembleDebug` attempted: wrapper could not download Gradle 8.11.1 (Network is unreachable). No Android compilation/lint or APK/AAB generation completed for this source. This environment also lacks the Android SDK and full JDK build setup.

## Next verification
With Java 17 JDK, Node 22+, SDK 36/build-tools 35.0.0 and Gradle available, run `sh gradlew --no-daemon --console=plain testDebugUnitTest lintDebug assembleDebug`. Install that new Preview and execute NEXT_ACCEPTANCE_MILESTONE.md. The October 16 APK remains useful for its existing camera permission tests, but contains none of this checkpoint's client message change. Live Gemini hosting, reviewed privacy URL, physical-phone testing, production signing and Play submission remain pending.
