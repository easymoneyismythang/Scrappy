# Camera recovery continuation — October 12, 2026 UTC

Continued from Library version 6 (preserving the newer gateway timeout fixes) of the camera/weather fixes package.

Changes:
- Both asynchronous photo failures and synchronous capture-start failures expose the manual identification helper. The camera previously suggested that helper while leaving its button hidden.
- Stopping the camera screen dismisses an unanswered upload consent dialog. Returning to the app requires a new prompt; the positive action also requires the activity to be resumed.
- Capture failures use a readable retry message rather than displaying internal exception text.

Verification actually performed:
- Gateway tests: 19 passed, zero failures; fake provider only.
- Standalone Kotlin core suite: 137 tests passed, zero failures. This excludes Android activities and SharedPreferences adapters.
- Release configuration and document consistency guards: passed.
- Android test/lint/debug build attempted before and after this camera edit. Gradle 8.11.1 could not download: Network is unreachable. Android changes have not been compiled or device-tested.

Device acceptance still required:
1. Trigger a capture error; verify Retry works and the identification helper is visible and opens.
2. Open AI consent, press Home, return: the old prompt must be closed; Analyze must show a new prompt and no upload should have occurred.
3. Repeat with another app covering Scrappy, with screen locking, and with rotation.
4. Accept consent while resumed and verify one request and normal results/error recovery.
5. Run the existing PRELAUNCH_TEST_MATRIX.md and capture orientation/review tests.

No APK/AAB is included. The existing GitLab pipeline remains the path to a current test APK when dependency downloads are available. Production AI/privacy configuration, signing, physical-phone acceptance and Play submission remain unfinished.
