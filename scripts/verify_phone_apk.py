#!/usr/bin/env python3
"""Verify a preview APK using Android SDK tools; optionally compare an older APK.

Read-only: does not install/uninstall apps or change phone data.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import zipfile


def run_tool(command):
    result = subprocess.run(command, text=True, capture_output=True, timeout=120)
    if result.returncode:
        raise ValueError(f'{Path(command[0]).name} failed: {result.stderr.strip() or result.stdout.strip()}')
    return result.stdout


def inspect_apk(path, aapt, apksigner):
    path = Path(path)
    with zipfile.ZipFile(path) as archive:
        damaged = archive.testzip()
        if damaged:
            raise ValueError(f'Corrupt APK entry: {damaged}')
        for required in ('AndroidManifest.xml', 'classes.dex'):
            if required not in archive.namelist():
                raise ValueError(f'APK missing {required}')
    # Presence of a signing block alone does not validate its cryptographic signature.
    signature = run_tool([apksigner, 'verify', '--verbose', '--print-certs', str(path)])
    certificates = sorted(set(re.findall(r'^Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]{64})\s*$', signature, re.M)))
    if not certificates:
        raise ValueError('No verified signer certificate fingerprint returned')
    badging = run_tool([aapt, 'dump', 'badging', str(path)])
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'", badging, re.M)
    minimum = re.search(r"^sdkVersion:'(\d+)'", badging, re.M)
    if not package or not minimum:
        raise ValueError('Could not read APK package/version/minimum SDK')
    return {
        'filename': path.name,
        'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
        'size_bytes': path.stat().st_size,
        'package': package[1], 'version_code': int(package[2]),
        'version_name': package[3], 'min_sdk': int(minimum[1]),
        'signer_sha256': [c.lower() for c in certificates],
        'signature_verified': True,
    }


def update_blockers(current, previous):
    if current['package'] != previous['package']:
        return ['Different package names: these APKs install as separate apps.']
    problems = []
    if current['signer_sha256'] != previous['signer_sha256']:
        problems.append('Signing certificates differ. An ordinary update will fail; rebuild with the original key. Preserve app data before considering uninstalling.')
    if current['version_code'] < previous['version_code']:
        problems.append('Version code is lower than the older APK. Build with a version code at least as high as the installed app.')
    return problems


def device_blockers(report, device_sdk):
    if device_sdk < report['min_sdk']:
        return [f"Phone Android API {device_sdk} is below this APK's minimum API {report['min_sdk']}. This APK cannot install on that Android version."]
    return []


def positive_sdk(value):
    try:
        sdk = int(value)
    except ValueError:
        raise argparse.ArgumentTypeError('Android API level must be a positive integer')
    if sdk < 1:
        raise argparse.ArgumentTypeError('Android API level must be a positive integer')
    return sdk


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--aapt', default='aapt')
    parser.add_argument('--apksigner', default='apksigner')
    parser.add_argument('--compare', type=Path, help='Older APK, ideally the exact one installed on the phone')
    parser.add_argument('--device-sdk', type=positive_sdk,
                        help='Phone Android API level (for example 26 for Android 8.0); not the Android version number')
    parser.add_argument('--variant', choices=('preview', 'camera-test'), default='preview',
                        help='Expected test app identity; camera-test installs beside the ordinary Preview')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    try:
        report = inspect_apk(args.apk, args.aapt, args.apksigner)
        expected_package = {'preview': 'com.scrappy.app.preview',
                            'camera-test': 'com.scrappy.app.cameratest'}[args.variant]
        if report['package'] != expected_package:
            raise ValueError(f'Expected the {args.variant} package {expected_package}')
        report['variant'] = args.variant
        report['phone_acceptance'] = 'Not tested; APK verification does not establish successful installation or camera/AI operation.'
        blockers = []
        if args.device_sdk is not None:
            report['device_sdk'] = args.device_sdk
            device_findings = device_blockers(report, args.device_sdk)
            report['device_findings'] = device_findings
            blockers.extend(device_findings)
        if args.compare:
            previous = inspect_apk(args.compare, args.aapt, args.apksigner)
            report['compared_apk'] = previous
            comparison_findings = update_blockers(report, previous)
            report['comparison_findings'] = comparison_findings
            blockers.extend(comparison_findings)
        encoded = json.dumps(report, indent=2) + '\n'
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(encoded)
        print(encoded, end='')
        return 1 if blockers else 0
    except (OSError, ValueError, zipfile.BadZipFile, subprocess.TimeoutExpired) as error:
        print(f'APK VERIFICATION FAILED: {error}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
