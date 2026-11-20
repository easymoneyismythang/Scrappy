import tempfile
import contextlib
import io
import json
from pathlib import Path
import unittest
from unittest.mock import patch
import zipfile

from verify_phone_apk import inspect_apk, update_blockers, device_blockers, main


class PhoneApkTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.apk = Path(self.tmp.name) / 'preview.apk'
        with zipfile.ZipFile(self.apk, 'w') as archive:
            archive.writestr('AndroidManifest.xml', b'test manifest')
            archive.writestr('classes.dex', b'test dex')
        self.cert = 'ab' * 32
        self.signature = f'Signer #1 certificate SHA-256 digest: {self.cert}\n'
        self.badging = "package: name='com.scrappy.app.preview' versionCode='55' versionName='4.9.6' platformBuildVersionName='16'\nsdkVersion:'26'\n"

    def inspect(self):
        with patch('verify_phone_apk.run_tool', side_effect=[self.signature, self.badging]):
            return inspect_apk(self.apk, 'aapt', 'apksigner')

    def test_verified_metadata(self):
        report = self.inspect()
        self.assertEqual(report['version_code'], 55)
        self.assertEqual(report['min_sdk'], 26)
        self.assertEqual(report['signer_sha256'], [self.cert])
        self.assertEqual(len(report['sha256']), 64)

    def test_invalid_signature_is_rejected_before_metadata(self):
        with patch('verify_phone_apk.run_tool', side_effect=ValueError('signature invalid')) as tool:
            with self.assertRaisesRegex(ValueError, 'signature invalid'):
                inspect_apk(self.apk, 'aapt', 'apksigner')
            self.assertEqual(tool.call_count, 1)

    def test_missing_fingerprint_is_rejected(self):
        self.signature = 'Verifies\n'
        with self.assertRaisesRegex(ValueError, 'fingerprint'):
            self.inspect()

    def test_not_an_apk_is_rejected(self):
        self.apk.write_bytes(b'not a zip')
        with self.assertRaises(zipfile.BadZipFile):
            self.inspect()

    def test_same_key_equal_version_is_allowed(self):
        current = self.inspect()
        self.assertEqual(update_blockers(current, dict(current)), [])

    def test_different_key_and_downgrade_both_reported(self):
        current = self.inspect()
        previous = dict(current, signer_sha256=['cd' * 32], version_code=56)
        findings = update_blockers(current, previous)
        self.assertEqual(len(findings), 2)
        self.assertIn('Signing certificates differ', findings[0])
        self.assertIn('Version code is lower', findings[1])

    def test_different_package_is_not_an_update(self):
        current = self.inspect()
        previous = dict(current, package='com.scrappy.app')
        self.assertIn('separate apps', update_blockers(current, previous)[0])

    def test_device_below_minimum_is_blocked(self):
        self.assertIn('cannot install', device_blockers(self.inspect(), 25)[0])

    def test_device_at_or_above_minimum_has_no_sdk_blocker(self):
        for sdk in (26, 37):
            self.assertEqual(device_blockers(self.inspect(), sdk), [])

    def test_cli_preserves_device_and_update_findings(self):
        current = self.inspect()
        previous = dict(current, signer_sha256=['cd' * 32], version_code=56)
        output = Path(self.tmp.name) / 'report.json'
        with patch('sys.argv', ['verify_phone_apk', str(self.apk), '--device-sdk', '25',
                                '--compare', str(self.apk), '--output', str(output)]), \
                patch('verify_phone_apk.inspect_apk', side_effect=[current, previous]), \
                contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(main(), 1)
        report = json.loads(output.read_text())
        self.assertEqual(report['device_sdk'], 25)
        self.assertEqual(len(report['device_findings']), 1)
        self.assertEqual(len(report['comparison_findings']), 2)
        self.assertIn('Not tested', report['phone_acceptance'])

    def test_cli_supported_sdk_is_not_phone_acceptance(self):
        output = io.StringIO()
        with patch('sys.argv', ['verify_phone_apk', str(self.apk), '--device-sdk', '26']), \
                patch('verify_phone_apk.inspect_apk', return_value=self.inspect()), \
                contextlib.redirect_stdout(output):
            self.assertEqual(main(), 0)
        report = json.loads(output.getvalue())
        self.assertEqual(report['device_findings'], [])
        self.assertIn('Not tested', report['phone_acceptance'])

    def test_cli_rejects_invalid_sdk_before_inspection(self):
        for value in ('0', '-1', 'eight', '26.5'):
            with self.subTest(value=value), \
                    patch('sys.argv', ['verify_phone_apk', str(self.apk), '--device-sdk', value]), \
                    patch('verify_phone_apk.inspect_apk') as inspect, \
                    contextlib.redirect_stderr(io.StringIO()), \
                    self.assertRaises(SystemExit) as error:
                main()
            self.assertEqual(error.exception.code, 2)
            inspect.assert_not_called()

    def test_camera_test_identity_requires_explicit_variant(self):
        current = dict(self.inspect(), package='com.scrappy.app.cameratest')
        for options, expected in (([], 1), (['--variant', 'camera-test'], 0)):
            with self.subTest(options=options), \
                    patch('sys.argv', ['verify_phone_apk', str(self.apk)] + options), \
                    patch('verify_phone_apk.inspect_apk', return_value=current), \
                    contextlib.redirect_stdout(io.StringIO()), \
                    contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(main(), expected)

    def test_camera_test_variant_rejects_ordinary_preview(self):
        with patch('sys.argv', ['verify_phone_apk', str(self.apk), '--variant', 'camera-test']), \
                patch('verify_phone_apk.inspect_apk', return_value=self.inspect()), \
                contextlib.redirect_stdout(io.StringIO()), \
                contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(main(), 1)

    def test_camera_test_variant_rejects_production_package(self):
        current = dict(self.inspect(), package='com.scrappy.app')
        with patch('sys.argv', ['verify_phone_apk', str(self.apk), '--variant', 'camera-test']), \
                patch('verify_phone_apk.inspect_apk', return_value=current), \
                contextlib.redirect_stdout(io.StringIO()), \
                contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(main(), 1)


if __name__ == '__main__':
    unittest.main()
