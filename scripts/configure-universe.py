"""Populate client configuration from Nuvio's public discovery document.

Only the public anon/publishable key belongs in a mobile app. No account
password, access token, service-role key, or private user data is used here.
"""
import base64
import json
import os
from pathlib import Path
import urllib.request
from urllib.parse import urlsplit

DISCOVERY_URL = 'https://api.nuvio.tv/.well-known/nuvio'


def validate_public_key(key):
    if key.startswith('sb_publishable_'):
        return
    try:
        payload = key.split('.')[1]
        claims = json.loads(base64.urlsafe_b64decode(payload + '=' * (-len(payload) % 4)))
    except (ValueError, IndexError):
        raise ValueError('Expected a public Supabase anon/publishable client key') from None
    if claims.get('role') != 'anon':
        raise ValueError('Only a public anon/publishable key may be embedded in this app')


def official_configuration(document):
    if document.get('version') != 1 or document.get('service') != 'nuvio':
        raise ValueError('Unsupported Nuvio discovery document')
    url = document.get('backend_url', '').rstrip('/')
    if url != 'https://api.nuvio.tv':
        raise ValueError('Official discovery did not point at the expected Nuvio server')
    if not document.get('capabilities', {}).get('email_password_auth'):
        raise ValueError('Nuvio discovery did not advertise password authentication')
    key = document.get('publishable_key', '').strip()
    validate_public_key(key)
    return {'NUVIO_SUPABASE_URL': url, 'NUVIO_SUPABASE_ANON_KEY': key}


def fetch_json(url, headers=None):
    request = urllib.request.Request(url, headers={'User-Agent': 'NuvioUniverse-Build', **(headers or {})})
    with urllib.request.urlopen(request, timeout=25) as response:
        return json.load(response)


def configure():
    path = Path('local.properties')
    existing = path.read_text() if path.exists() else ''
    props = {}
    for line in existing.splitlines():
        if '=' in line and not line.lstrip().startswith(('#', '!')):
            key, value = line.split('=', 1)
            props[key.strip()] = value.strip()
    for key in ('TMDB_API_KEY', 'MDBLIST_CLIENT_ID', 'NUVIO_SUPABASE_URL', 'NUVIO_SUPABASE_ANON_KEY', 'NUVIO_SUPABASE_FALLBACK_URL'):
        if os.environ.get(key):
            props[key] = os.environ[key]
    if not props.get('NUVIO_SUPABASE_URL') or not props.get('NUVIO_SUPABASE_ANON_KEY'):
        props.update(official_configuration(fetch_json(DISCOVERY_URL)))
    validate_public_key(props['NUVIO_SUPABASE_ANON_KEY'])
    if urlsplit(props['NUVIO_SUPABASE_URL']).scheme != 'https':
        raise ValueError('Account configuration must use HTTPS')
    # Read-only check: validates the actual endpoint/key without attempting a login.
    settings = fetch_json(props['NUVIO_SUPABASE_URL'].rstrip('/') + '/auth/v1/settings',
                          {'apikey': props['NUVIO_SUPABASE_ANON_KEY']})
    if not settings.get('external', {}).get('email'):
        raise ValueError('The configured server did not enable email sign-in')
    if os.environ.get('RELEASE') == 'true':
        required = ('SIGNING_KEY', 'SIGNING_PASSWORD', 'SIGNING_ALIAS', 'KEY_PASSWORD')
        if any(not os.environ.get(key) for key in required):
            raise ValueError('Add the four UNIVERSE signing secrets before a release build')
        signing = Path(os.environ['RUNNER_TEMP']) / 'universe.jks'
        signing.write_bytes(base64.b64decode(os.environ['SIGNING_KEY'], validate=True))
        props.update(NUVIO_RELEASE_STORE_FILE=str(signing),
                     NUVIO_RELEASE_STORE_PASSWORD=os.environ['SIGNING_PASSWORD'],
                     NUVIO_RELEASE_KEY_ALIAS=os.environ['SIGNING_ALIAS'],
                     NUVIO_RELEASE_KEY_PASSWORD=os.environ['KEY_PASSWORD'])
    def escape(value):
        return value.replace('\\', '\\\\').replace('\n', '\\n').replace('\r', '\\r')
    path.write_text(''.join(f'{key}={escape(value)}\n' for key, value in props.items()))
    print('Nuvio account endpoint configured; read-only auth-settings check passed. No user login attempted.')


if __name__ == '__main__':
    configure()
