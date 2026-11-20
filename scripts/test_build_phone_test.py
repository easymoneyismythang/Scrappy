import contextlib
import io
import os
import unittest
from email.message import Message
from unittest.mock import MagicMock
from unittest.mock import patch

import build_phone_test as build


class LiveBuildTest(unittest.TestCase):
    def settings(self, endpoint='https://ai.scrappy-demo.com/v1/identify', privacy='https://www.scrappy-demo.com/privacy'):
        return {'SCRAPPY_AI_ENDPOINT': endpoint, 'SCRAPPY_AI_PRIVACY_URL': privacy}

    def test_offline_overrides_stale_properties(self):
        command = build.build_command({})
        self.assertIn('-PscrappyAiEndpoint=', command)
        self.assertIn('-PscrappyAiPrivacyUrl=', command)

    def test_live_urls_are_forwarded_as_separate_arguments(self):
        settings = self.settings()
        command = build.build_command(settings)
        self.assertIn('-PscrappyAiEndpoint=' + settings['SCRAPPY_AI_ENDPOINT'], command)
        self.assertIn('-PscrappyAiPrivacyUrl=' + settings['SCRAPPY_AI_PRIVACY_URL'], command)
        self.assertIn('assembleCameraTest', command)
        self.assertIn('testCameraTestUnitTest', command)

    def test_partial_configuration_fails(self):
        for endpoint, privacy in [('', 'https://scrappy-demo.com/privacy'), ('https://scrappy-demo.com/v1/identify', '')]:
            with self.subTest(endpoint=endpoint), self.assertRaises(ValueError):
                build.build_command(self.settings(endpoint, privacy))

    def test_unsafe_urls_in_either_field_fail_without_exposing_value(self):
        unsafe = ['http://scrappy-demo.com/v1/identify',
                  'HTTPS://scrappy-demo.com/v1/identify',
                  'https://scrappy-demo.com:/v1/identify',
                  'https://scrappy-demo.com/privacy%zz',
                  'https://secret-key@scrappy-demo.com/v1/identify',
                  'https://scrappy-demo.com:8443/v1/identify',
                  'https://scrappy-demo.com/v1/identify?key=secret-key',
                  'https://scrappy-demo.com/v1/identify#secret-key',
                  'https://scrappy-demo.com/v1/identify?',
                  'https://scrappy-demo.com/v1/identify\n',
                  'https://scrappy-demo.com/"secret-key',
                  'https://scrappy-demo.com/\\secret-key',
                  'https://scrappy-demo.com:wrong/v1/identify',
                  'https://127.0.0.1/v1/identify', 'https://[::1]/v1/identify',
                  'https://localhost/v1/identify', 'https://example.com/v1/identify',
                  'https://gateway.local/v1/identify', 'https://bad..host.com/v1/identify']
        for value in unsafe:
            for field in ('SCRAPPY_AI_ENDPOINT', 'SCRAPPY_AI_PRIVACY_URL'):
                with self.subTest(value=value, field=field):
                    settings = self.settings()
                    settings[field] = value
                    with self.assertRaises(ValueError) as caught:
                        build.build_command(settings)
                    self.assertNotIn('secret-key', str(caught.exception))

    def test_endpoint_requires_exact_route(self):
        for path in ('', '/v1/identify/', '/v1/%69dentify', '/other'):
            with self.subTest(path=path), self.assertRaises(ValueError):
                build.build_command(self.settings(endpoint='https://scrappy-demo.com' + path))

    def test_default_https_port_and_encoded_privacy_path_work(self):
        build.build_command(self.settings('https://ai.scrappy-demo.com:443/v1/identify',
                                           'https://scrappy-demo.com/legal/privacy%20policy'))

    def test_check_only_does_not_launch_gradle(self):
        with patch.dict(os.environ, self.settings(), clear=True), patch.object(build.subprocess, 'run') as run:
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(0, build.main(['--check-only']))
            run.assert_not_called()

    def test_bad_configuration_does_not_launch_build(self):
        with patch.dict(os.environ, self.settings(privacy=''), clear=True), patch.object(build.subprocess, 'run') as run:
            with contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(2, build.main([]))
            run.assert_not_called()

    def test_build_failure_is_returned_to_ci(self):
        with patch.dict(os.environ, {}, clear=True), patch.object(build.subprocess, 'run') as run:
            run.return_value.returncode = 7
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(7, build.main([]))
            self.assertEqual(build.ROOT, run.call_args.kwargs['cwd'])
            self.assertNotIn('shell', run.call_args.kwargs)

    def responses(self, health=b'{"status":"ok"}', privacy=b'<html>Privacy</html>', content_type='text/html'):
        opener = MagicMock()
        responses = []
        for body in (health, privacy):
            response = MagicMock()
            response.status = 200
            response.read.side_effect = lambda limit, body=body: body[:limit]
            response.headers = Message()
            response.headers['Content-Type'] = content_type
            response.__enter__.return_value = response
            responses.append(response)
        opener.open.side_effect = responses
        return opener, responses

    def test_service_check_only_reads_health_and_privacy(self):
        opener, responses = self.responses()
        build.check_service(self.settings(), opener)
        calls = opener.open.call_args_list
        self.assertEqual(['https://ai.scrappy-demo.com/health', 'https://www.scrappy-demo.com/privacy'],
                         [call.args[0].full_url for call in calls])
        for call in calls:
            self.assertEqual('GET', call.args[0].get_method())
            self.assertIsNone(call.args[0].data)
            self.assertEqual(10, call.kwargs['timeout'])
        responses[0].read.assert_called_once_with(4097)
        responses[1].read.assert_called_once_with(1)

    def test_service_check_requires_live_configuration(self):
        opener, _ = self.responses()
        with self.assertRaisesRegex(ValueError, 'require both'):
            build.check_service({}, opener)
        opener.open.assert_not_called()

    def test_invalid_health_stops_before_privacy(self):
        for body in (b'not json', b'{"status":"down"}', b'x' * 4097):
            with self.subTest(body=body[:20]):
                opener, _ = self.responses(health=body)
                with self.assertRaisesRegex(ValueError, 'Gateway health check failed'):
                    build.check_service(self.settings(), opener)
                self.assertEqual(1, opener.open.call_count)

    def test_privacy_requires_nonempty_html(self):
        for body, content_type in [(b'', 'text/html'), (b'{}', 'application/json')]:
            opener, _ = self.responses(privacy=body, content_type=content_type)
            with self.assertRaisesRegex(ValueError, 'Privacy page check failed'):
                build.check_service(self.settings(), opener)

    def test_service_errors_do_not_print_addresses_or_credentials(self):
        opener, _ = self.responses()
        opener.open.side_effect = OSError('private-proxy-password https://private-address')
        with self.assertRaises(ValueError) as caught:
            build.check_service(self.settings(), opener)
        self.assertNotIn('private', str(caught.exception))

    def test_non_200_service_response_fails(self):
        for index in (0, 1):
            opener, responses = self.responses()
            responses[index].status = 503
            with self.assertRaisesRegex(ValueError, 'check failed'):
                build.check_service(self.settings(), opener)

    def test_service_check_never_launches_gradle(self):
        with patch.dict(os.environ, self.settings(), clear=True), patch.object(build, 'check_service') as check, patch.object(build.subprocess, 'run') as run:
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(0, build.main(['--check-service']))
            check.assert_called_once()
            run.assert_not_called()

    def test_failed_service_check_has_nonzero_exit_without_build(self):
        with patch.dict(os.environ, self.settings(), clear=True), patch.object(build, 'check_service', side_effect=ValueError('Gateway health check failed')), patch.object(build.subprocess, 'run') as run:
            with contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(2, build.main(['--check-service']))
            run.assert_not_called()

    def test_service_redirects_are_refused(self):
        handler = build.NoRedirect()
        self.assertIsNone(handler.redirect_request(None, None, 302, '', {}, 'http://unsafe.local'))


if __name__ == '__main__':
    unittest.main()
