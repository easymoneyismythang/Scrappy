# Scrappy v4.9.6 — continued work checkpoint

Date: 2026-11-03

## Source changes in this checkpoint
- Threaded the active `RequestCancellation` into `AiPhotoClient.reviewBitmap` when preparing the AI upload image.
- Added cancellation checks before and after image-bounds decoding, before pixel decoding, after pixel decoding, and after EXIF orientation lookup.
- Pixel decode is not itself interruptible; the post-decode cancellation check runs inside the cleanup-protected block so the decoded bitmap is recycled if cancellation arrived during decoding.
- Added offline source-guard assertions and a mutation test to catch removal of the cancellation handoff.

## Verification run for this checkpoint
- Python helper/verifier suite: 39 tests passed.
- Camera safety source guard: passed, 20 invariants.
- Release consistency check: passed.
- Release configuration wiring check: passed.
- AI gateway tests: 32 passed with the controlled test provider.
- Fresh Android camera-test unit-test task was attempted, but Gradle could not download `gradle-8.11.1-bin.zip` because `services.gradle.org` could not be resolved (`UnknownHostException`). Therefore the Kotlin change is not compiled or runtime-tested in this environment.

## Still required
1. Restore/push the full source tree to the intended private GitHub repository; the known `Scrappy-420` repository previously reported size 0.
2. Run `:app:testCameraTestUnitTest` and `connectedCameraTestAndroidTest` after Gradle access is restored.
3. On a physical Samsung device, verify capture, preview, cancel during photo preparation, retry, retake, and no stale result after cancel.
4. Configure the HTTPS Gemini gateway and privacy URL, then perform a live AI request using a non-sensitive scrap-metal photo.
5. Produce and verify a signed release APK/AAB before Play Console submission.

No APK/AAB was produced in this checkpoint. Do not treat source-level guards or mocked gateway tests as proof of Android runtime or live Gemini behavior.
