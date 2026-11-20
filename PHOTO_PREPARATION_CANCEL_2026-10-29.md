# Scrappy v4.9.6 — cancel photo preparation (October 29, 2026)

Cancel analysis now passes the request cancellation token into photo preparation.
Preparation checks before decoding, after decoding and before/after each JPEG
compression pass. A canceled request stops at the next checkpoint, releasing its
bitmap in the existing finally block, rather than finishing all compression
passes before it notices cancellation. The original cached photo is untouched.
The existing final pre-upload check and generation guard remain in place.
An Android bitmap operation already underway is not interrupted mid-operation;
a photo already sent to the service cannot be retracted.

Added two Android regressions: an already canceled request must fail before
reading even a missing photo, and canceling preparation must preserve a valid
original and allow preparation with a fresh request. These tests have NOT been
compiled or executed in this session. Existing success-path image tests continue
to cover callers that omit the optional cancellation token.

Verification executed in this session:
- Python build-helper/APK-verifier suite: 33 passed.
- Node gateway suite: 32 passed with a controlled provider.
- Release configuration and version consistency checks: passed.
- assembleCameraTest and assembleDebugAndroidTest: attempted, failed before
  compilation at Gradle 8.11.1 download (Network is unreachable).
- Android SDK, camera runtime, new Android regressions and live Gemini: unverified.
- No new APK/AAB produced. Earlier APKs do not contain this change.

Evidence: verification/photo-preparation-cancel-2026-10-29/.
Next build gate: compile current source and run connectedDebugAndroidTest on an
emulator or phone. After that, test cancel/retry with a detailed photo and a
configured test service. Production privacy/AI, signing and Play gates remain.
