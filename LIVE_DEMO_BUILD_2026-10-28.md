# Live demo build configuration — October 28, 2026

The GitHub and GitLab phone-build paths previously supplied no AI endpoint or
privacy URL to Gradle, so their APKs kept uploads disabled. The new shared
`scripts/build_phone_test.py` reads paired public URL environment variables,
validates them before launching Gradle, and passes explicit properties to both
test app variants. Empty values explicitly override stale local Gradle values.
There is no Gemini key in these arguments. Validation errors omit rejected
values to avoid printing accidentally pasted credentials.

GitLab now also packages Camera Test and verifies both APK signatures/package
identities, matching the existing GitHub handoff. Both workflows run the new
configuration tests. `LIVE_DEMO_SETUP.md` explains the hosting requirements,
repository variables, Android artifact selection and live acceptance steps.

Verification in this session:

- Nine new build-configuration tests passed: paired settings, offline reset,
  rejected credentials/query/fragment/control characters, malformed/placeholder
  URLs, HTTPS/default-port enforcement, exact endpoint route, no Gradle launch
  on invalid input/check-only, and propagation of build failures.
- All 15 existing APK-verifier tests passed (24 Python tests total).
- All 32 gateway tests passed (56 automated tests total).
- Release configuration and release consistency checks passed.
- Both modified workflow YAML files parsed successfully.
- Offline `--check-only` completed successfully.

Evidence is under `verification/live-demo-2026-10-28/`.

No Android source was changed. This environment has no configured Android SDK;
no Gradle Android build, newly signed APK, remote CI execution, physical phone
test, live Gemini request or deployment is claimed. Existing Kotlin test results
remain historical. Real hosting, provider setup and the paired public addresses
are still required before building and accepting a live-demo APK.
