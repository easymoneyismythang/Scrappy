#!/usr/bin/env python3
"""Fail fast when current release handoff docs disagree with Android metadata."""
from pathlib import Path
import re, sys
root = Path(__file__).resolve().parents[1]
gradle = (root/'app/build.gradle.kts').read_text()
name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
code = re.search(r'versionCode\s*=\s*(\d+)', gradle)
if not name or not code:
    print('FAIL: could not parse Android version metadata'); sys.exit(1)
version, version_code = name.group(1), code.group(1)
errors=[]
for rel in ['README.md','INSTALL_TEST_BUILD.md','NEXT_ACCEPTANCE_MILESTONE.md','RELEASE_AUDIT_4.9.6.md','CURRENT_VERIFICATION_4.9.6.md','BUILD_AND_SIGN.md','PLAY_STORE_RELEASE_CHECKLIST.md','STORE_LISTING_DRAFT.md','DATA_SAFETY_WORKSHEET_4.9.6.md','PLAY_CONSOLE_SUBMISSION_4.9.6.md']:
    p=root/rel
    if not p.exists(): errors.append(f'missing {rel}'); continue
    text=p.read_text()
    if version not in text: errors.append(f'{rel}: missing current version {version}')
install=(root/'INSTALL_TEST_BUILD.md').read_text()
if f'versionCode `{version_code}`' not in install:
    errors.append(f'INSTALL_TEST_BUILD.md: missing versionCode {version_code}')
builddoc=(root/'BUILD_AND_SIGN.md').read_text()
if f'versionCode `{version_code}`' not in builddoc:
    errors.append(f'BUILD_AND_SIGN.md: missing versionCode {version_code}')
if 'Fresh Android verification required before Play upload' not in builddoc:
    errors.append('BUILD_AND_SIGN.md: missing fresh-verification warning')
nextdoc=(root/'NEXT_ACCEPTANCE_MILESTONE.md').read_text()
if 'older v4.9.0 AAB/APK' not in nextdoc:
    errors.append('NEXT_ACCEPTANCE_MILESTONE.md: missing stale-artifact warning')
if errors:
    print('RELEASE CONSISTENCY: FAIL')
    print('\n'.join('- '+e for e in errors)); sys.exit(1)
print(f'RELEASE CONSISTENCY: PASS — Android {version} ({version_code}) handoff docs aligned')
