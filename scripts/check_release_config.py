#!/usr/bin/env python3
from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app'/'build.gradle.kts'
s=p.read_text()
checks={
 'release validation task':'validateScrappyReleaseConfig',
 'AI endpoint required':'scrappyAiEndpoint',
 'privacy URL required':'scrappyAiPrivacyUrl',
 'HTTPS enforcement':'uri.scheme == "https"',
 'TLS port restriction':'uri.port in listOf(-1, 443)',
 'AI path restriction':'"/v1/identify"',
 'release bundle gate':'name == "bundleRelease"',
 'release APK gate':'name == "assembleRelease"',
 'shared release build gate':'name == "preReleaseBuild"',
}
missing=[label for label,needle in checks.items() if needle not in s]
if missing:
    raise SystemExit('FAIL: '+', '.join(missing))
print('PASS: release configuration gate is wired for AI endpoint + privacy URL safety')
