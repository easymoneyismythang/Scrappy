# Scrappy live camera demo — v4.9.6

The phone build pipelines now accept your live server addresses. The existing
downloaded Camera Test APK still has AI disabled; changing pipeline settings
requires building and installing a new APK. This update contains source, not
a newly built APK or a deployed service.

## What is needed

1. A hosted Scrappy gateway with an HTTPS address ending in `/v1/identify`.
2. A published privacy policy describing the real hosting and Google image
   processing arrangements, reachable at its own HTTPS URL.
3. Gemini credentials configured only on that server, with an available vision
   model selected for your account. Never enter an API key into the app or the
   phone-build variables below.

The included gateway runs on Node 22+ and needs `GEMINI_API_KEY`,
`GEMINI_MODEL`, `BUDGET_FILE` (absolute persistent private file path), and a
chosen `DAILY_REQUEST_LIMIT`. Use one process with persistent storage. Start
with `npm start` inside `backend`; see `backend/README.md` for the gateway's
limits, reverse proxy requirements and acceptance checks. Configure HOST/PORT
for the hosting service. No hosting account or live provider was provisioned
by this source update.

## Build from your phone

The project must first be uploaded to a repository that you can access, with
its source files extracted into the repository root. Uploading only this ZIP
does not activate either build pipeline.

For GitLab, add these two project CI/CD variables. For GitHub, add these two
repository Actions variables (the workflow reads `vars`, not `secrets`):

| Variable | Value |
| --- | --- |
| `SCRAPPY_AI_ENDPOINT` | Your actual public HTTPS URL ending exactly in `/v1/identify` |
| `SCRAPPY_AI_PRIVACY_URL` | Your actual public HTTPS privacy-policy URL |

Both URLs become public app configuration. Do not put credentials, tokens,
query strings, fragments or nonstandard ports in either value. Use real DNS
hostnames. Local addresses, IP literals and placeholder domains are rejected.
If GitLab variables are protected, run the build on a protected branch that
receives them; otherwise they may be unavailable to the job.

Run the pipeline for the updated source. On GitHub the workflow is **Build
Scrappy phone APK**, and the separate download is **Scrappy-camera-test**.
On GitLab open the `build_phone_test_apk` job's artifacts and download
`app/build/outputs/apk/cameraTest/app-cameraTest.apk`. Extract any artifact ZIP
before tapping the APK on Android.

Install and open **Scrappy Camera Test**. Preserve your existing Scrappy app
and its data. If Android rejects an update due to a different debug signing
key, export any needed Camera Test ledger data before removing only that test
app, then install the new one. The production app and Preview have separate
package identities.

Leaving both variables empty builds an offline camera test. Setting only one,
or using an invalid URL, stops the build instead of producing a partially
configured app. Successful configuration validation proves syntax only, not
server reachability or Gemini operation.

## Confirm the live demo

Before building, run **Check Scrappy live setup** from GitHub Actions, or
start the optional **check_live_setup** GitLab job. These use the same two
public URL variables. This diagnostic checks the gateway's `/health` response
and that the privacy address returns nonempty HTML. It sends no photos,
does not call Gemini and does not build an APK. It fails if either setting
is missing, TLS verification fails, a page redirects or a response is not
HTTP 200. Configure direct final addresses; `/health` must be at the gateway
origin's root. The CI job has a three-minute overall timeout.

A passing check confirms setup reachability from that runner only. It does
not review the policy text, certify TLS/proxy settings, verify Android
connectivity or prove that Gemini is configured correctly. Continue with
the real phone acceptance below.

1. Open the privacy link and confirm that the published policy loads.
2. Capture a clear photo of one non-sensitive item. Review the disclosure and
   consent before sending it.
3. Confirm a broad metal-group estimate or an honest Unknown result appears.
   A photo cannot certify an alloy or a yard's payable grade.
4. Decline consent on another attempt, then test retake/back and network loss.
   Check that errors allow recovery rather than leaving the screen stuck.
5. Check the gateway's count-only budget survives a restart. Review hosting
   logging and the selected provider's actual retention settings.

Do not regard `/health` returning `ok` as a successful Gemini test; it checks
only process liveness. Record real phone/provider acceptance separately from
CI tests. Public rollout and Google Play signing/submission remain separate.

## Local developer command

Export the same two public URL variables, then run:

```bash
python3 scripts/build_phone_test.py --check-only
python3 scripts/build_phone_test.py --check-service
python3 scripts/build_phone_test.py
```

The first command performs no network requests or build. The second performs
the read-only health/privacy diagnostic. Each HTTP connection/read has a
10-second timeout; use the CI job for an overall three-minute deadline.
The third runs
unit tests, lint and packaging for both Preview and Camera Test, plus the
Preview instrumentation APK build. It does not execute instrumentation on a
phone. Python 3, Java 17, Android SDK 36/build-tools 35.0.0, Node 22+ and Gradle
dependency access are required. CI also checks signatures and package identity.
