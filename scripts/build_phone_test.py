#!/usr/bin/env python3
"""Build phone APKs, optionally wired to a live AI gateway using public URLs."""
import argparse
import ipaddress
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import urlsplit
from urllib.request import Request, build_opener, HTTPSHandler, HTTPRedirectHandler

ROOT = Path(__file__).resolve().parents[1]
TASKS = ['testDebugUnitTest', 'lintDebug', 'assembleDebug', 'assembleDebugAndroidTest',
         'testCameraTestUnitTest', 'lintCameraTest', 'assembleCameraTest']


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def check_service(env, opener=None):
    """Check public setup without sending photos, credentials or provider calls."""
    build_command(env)
    endpoint = env.get('SCRAPPY_AI_ENDPOINT', '')
    if not endpoint:
        raise ValueError('Live service checks require both public URL settings')
    url = urlsplit(endpoint)
    health = f'https://{url.netloc}/health'
    opener = opener or build_opener(HTTPSHandler(), NoRedirect())
    for name, address in [('Gateway health', health),
                          ('Privacy page', env['SCRAPPY_AI_PRIVACY_URL'])]:
        try:
            request = Request(address, headers={'Accept': 'application/json' if name == 'Gateway health' else 'text/html',
                                                'User-Agent': 'Scrappy-setup-check/1'})
            with opener.open(request, timeout=10) as response:
                if response.status != 200:
                    raise ValueError('Unexpected status')
                if name == 'Gateway health':
                    body = response.read(4097)
                    if len(body) > 4096 or json.loads(body) != {'status': 'ok'}:
                        raise ValueError('Unexpected health response')
                else:
                    if response.headers.get_content_type() != 'text/html' or not response.read(1):
                        raise ValueError('Privacy page must contain HTML')
        except Exception:
            # HTTP/TLS errors can contain addresses or proxy credentials. Keep
            # the diagnostic actionable without copying private environment data.
            raise ValueError(f'{name} check failed; verify HTTPS, the exact address and a direct HTTP 200 response') from None


def public_url(value, field, required_path=None):
    # Restrict build-time string content as well as URL syntax. Do not echo a
    # rejected value: someone might accidentally paste a credential here.
    error = ValueError(f'{field} must be a public HTTPS URL on port 443, without credentials, query or fragment')
    if (not value.startswith('https://') or
            re.search(r'[\s\x00-\x1f\x7f"\\<>]', value) or
            re.search(r'%(?![0-9a-fA-F]{2})', value)):
        raise error
    try:
        url = urlsplit(value)
        host = url.hostname
        port = url.port
    except ValueError:
        raise error from None
    if (url.scheme != 'https' or not host or url.username is not None or
            url.password is not None or port not in (None, 443) or '?' in value or '#' in value or
            not re.fullmatch(r'[a-zA-Z0-9.-]+(?::443)?', url.netloc)):
        raise error
    # Android's endpoint parser requires a DNS host, and phone tests need a
    # reachable service rather than localhost or a private development address.
    if (not re.fullmatch(r'[a-zA-Z0-9](?:[a-zA-Z0-9.-]*[a-zA-Z0-9])?', host) or
            any(not label or len(label) > 63 or label.startswith('-') or label.endswith('-')
                for label in host.split('.')) or len(host) > 253 or '.' not in host or
            host.endswith(('.localhost', '.local', '.internal', '.test', '.invalid', '.example')) or
            host in ('example.com', 'example.net', 'example.org')):
        raise error
    try:
        ipaddress.ip_address(host)
    except ValueError:
        pass
    else:
        raise error
    if required_path is not None and url.path != required_path:
        raise ValueError(f'{field} must use the exact /v1/identify path')
    return value


def build_command(env):
    endpoint = env.get('SCRAPPY_AI_ENDPOINT', '')
    privacy = env.get('SCRAPPY_AI_PRIVACY_URL', '')
    if bool(endpoint) != bool(privacy):
        raise ValueError('Set both SCRAPPY_AI_ENDPOINT and SCRAPPY_AI_PRIVACY_URL, or leave both empty for offline testing')
    if endpoint:
        public_url(endpoint, 'SCRAPPY_AI_ENDPOINT', '/v1/identify')
        public_url(privacy, 'SCRAPPY_AI_PRIVACY_URL')
    # Pass even empty values, overriding any stale local Gradle properties.
    # Arguments are never evaluated by a shell. Only public URLs enter the APK.
    return ['sh', str(ROOT / 'gradlew'), '--no-daemon', '--console=plain',
            f'-PscrappyAiEndpoint={endpoint}', f'-PscrappyAiPrivacyUrl={privacy}', *TASKS]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    mode_args = parser.add_mutually_exclusive_group()
    mode_args.add_argument('--check-only', action='store_true', help='Validate URL settings without building or contacting a service')
    mode_args.add_argument('--check-service', action='store_true', help='Check HTTPS health/privacy pages without uploading photos or building')
    args = parser.parse_args(argv)
    try:
        command = build_command(os.environ)
        if args.check_service:
            check_service(os.environ)
    except ValueError as error:
        print(f'Configuration error: {error}', file=sys.stderr)
        return 2
    mode = 'live AI configured; provider and phone acceptance still required' if os.environ.get('SCRAPPY_AI_ENDPOINT') else 'offline camera testing; AI uploads disabled'
    print(f'Scrappy build mode: {mode}', flush=True)
    if args.check_service:
        print('Gateway health and privacy page passed. Live Gemini and phone acceptance are still required.')
        return 0
    if args.check_only:
        return 0
    return subprocess.run(command, cwd=ROOT, check=False).returncode


if __name__ == '__main__':
    sys.exit(main())
