import base64
import importlib.util
from pathlib import Path
import unittest
import json

spec = importlib.util.spec_from_file_location('configuration', Path(__file__).with_name('configure-universe.py'))
configuration = importlib.util.module_from_spec(spec)
spec.loader.exec_module(configuration)


class ConfigurationTest(unittest.TestCase):
    def public_key(self, role):
        return 'header.' + base64.urlsafe_b64encode(json.dumps({'role': role}).encode()).decode().rstrip('=') + '.signature'

    def document(self):
        return {'version': 1, 'service': 'nuvio', 'backend_url': 'https://api.nuvio.tv',
                'publishable_key': self.public_key('anon'), 'capabilities': {'email_password_auth': True}}

    def test_official_public_configuration(self):
        config = configuration.official_configuration(self.document())
        self.assertEqual('https://api.nuvio.tv', config['NUVIO_SUPABASE_URL'])
        self.assertTrue(config['NUVIO_SUPABASE_ANON_KEY'])

    def test_private_service_role_key_is_rejected(self):
        with self.assertRaises(ValueError):
            configuration.validate_public_key(self.public_key('service_role'))

    def test_missing_or_malformed_key_is_rejected(self):
        for key in ('', 'not-a-key'):
            with self.assertRaises(ValueError):
                configuration.validate_public_key(key)

    def test_wrong_destination_is_rejected(self):
        document = self.document()
        document['backend_url'] = 'https://example.org'
        with self.assertRaises(ValueError):
            configuration.official_configuration(document)

    def test_missing_auth_support_is_rejected(self):
        document = self.document()
        document['capabilities'] = {}
        with self.assertRaises(ValueError):
            configuration.official_configuration(document)


if __name__ == '__main__':
    unittest.main()
