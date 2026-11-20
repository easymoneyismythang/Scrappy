# Scrappy 4.9.6 — phone APK verification

October 21, 2026 (America/New_York). Source checkpoint, version code 55 unchanged.

Added a read-only APK verifier and integrated it into the phone-build workflow before APK upload. Android SDK tools must successfully verify the signature and return package/version metadata. The report records the APK hash and signing certificate fingerprints. Optional comparison against an older APK flags changed signing keys, package differences and version downgrades. No app data is changed.

Validation performed:
- Seven Python regression tests passed for the new verifier's orchestration and comparison logic using controlled tool output. These are not real APK signature tests.
- All 27 Node gateway tests passed.
- Release configuration and version consistency checks passed.
- Android test/lint/build attempted; Gradle distribution download failed with `java.net.SocketException: Network is unreachable`. Android SDK tools are also unavailable. No new APK was produced, and no fresh Kotlin/Android verification or physical-phone test completed.
- Existing preview-3 APK passed ZIP CRC checks, has ARM64 native libraries and an APK signing block. This does not establish signature validity or identify the phone installation failure.

Next: build in an Android SDK environment; retain `verification/phone-apk.json` from CI; capture the exact phone installation error if it still fails. If a previously installed Preview exists, compare against that exact APK. Preserve data and recover the original signing key before considering uninstalling. Continue camera permission, capture and Gemini acceptance checks after installation succeeds.
