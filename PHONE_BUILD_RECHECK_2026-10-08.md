# Scrappy phone build recheck — October 8, 2026

Started from Scrappy-v4.9.6-phone-build-ready-fixed.zip.

## Change

The GitLab job used a Java-only container without installing the Android SDK. It now downloads Google's command-line tools with SHA-256 verification, installs Android SDK 36 and build tools 34.0.0, and runs Android unit tests and lint before packaging the preview APK.

## Verification

- Backend: 12 tests passed.
- Release configuration check: passed.
- Release/document consistency check: passed (4.9.6 / 55).
- Pipeline YAML and embedded shell commands: parsed and syntax checked.
- Local Android build: blocked before compilation because the Gradle download failed with Network is unreachable. Android SDK is also unavailable locally.
- Updated GitLab job: not executed against a remote runner in this session.

This package contains source, not a newly compiled APK. Production AI remains dependent on deployment/configuration of the gateway and public privacy URL. Physical-device testing is still pending.

## Phone route

Upload the ZIP contents, including .gitlab-ci.yml, to the root of the GitLab repository. Open Build > Pipelines, then the build_phone_test_apk job. A successful job provides app-debug.apk in its downloadable artifacts.
