# APK device compatibility checkpoint — October 27, 2026 (New York)

The read-only APK verifier now accepts an optional `--device-sdk` positive
integer. It reports a phone API level below the APK minimum SDK as an
installation blocker, saves the supplied level and findings in JSON, and exits
with status 1. Device and update findings are accumulated so a comparison
cannot hide an unsupported Android version. Invalid input fails before APK
inspection. Without this option, no phone SDK compatibility is inferred.

Verification: all 12 Python verifier tests passed, including minimum-boundary,
invalid-input, CLI JSON/exit-status, and simultaneous device/signature/downgrade
cases. SDK tool output is mocked in these tests. Release consistency and
release configuration checks passed. No real APK inspection or phone test was
performed in this session, and no new APK was built. Android source and version
remain unchanged. Latest compiled preview: October 26 photo-bounds checkpoint.
Live Gemini, physical-phone acceptance and production release gates remain open.
