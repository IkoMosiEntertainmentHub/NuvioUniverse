import os
from pathlib import Path
import subprocess
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch
import universe_version as version

class VersionTests(unittest.TestCase):
    def test_decimal_carries(self):
        for revision, label in [(0, '1.0.0'), (1, '1.0.1'), (9, '1.0.9'), (10, '1.1.0'), (20, '1.2.0'), (24, '1.2.4'), (100, '2.0.0')]:
            self.assertEqual(label, version.personal(revision))

    def test_identifier_preserves_upstream_label(self):
        self.assertEqual('NuvioUniverse-0.6.5-beta-(148)-v1.2.4', version.identifier('0.6.5-beta', 148, 24))
        self.assertEqual('NuvioUniverse-0.6.6-(149)-v1.2.4', version.identifier('0.6.6', 149, 24))

    def test_real_official_merge_then_personal_commit(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            def git(*args):
                return subprocess.check_output(['git', *args], cwd=root, text=True, stderr=subprocess.DEVNULL).strip()
            git('init', '-b', 'official')
            git('config', 'user.name', 'Version test')
            git('config', 'user.email', 'test@example.invalid')
            (root / 'upstream-version').write_text('0.6.5-beta (148)')
            git('add', '.'); git('commit', '-m', 'official base')
            git('switch', '-c', 'personal')
            (root / version.LEDGER).write_text('personalRevision=24\nreleaseClassification=release\n')
            (root / 'personal-code').write_text('v1')
            git('add', '.'); git('commit', '-m', 'personal base')
            git('switch', 'official')
            (root / 'upstream-version').write_text('0.6.6 (149)')
            git('add', '.'); git('commit', '-m', 'official update')
            git('update-ref', 'refs/remotes/upstream/cmp-rewrite', 'HEAD')
            git('switch', 'personal')
            git('merge', '--no-commit', '--no-ff', 'official')
            with patch.object(version, 'ROOT', root):
                version.prepare()
                self.assertEqual(24, version.current_revision())
                git('commit', '-m', 'incorporate official update')
                self.assertEqual(24, version.current_revision())
                (root / 'personal-code').write_text('v2')
                git('add', 'personal-code')
                version.prepare()
                self.assertEqual(25, version.current_revision())
                git('commit', '-m', 'new personal change')
                self.assertIn('personalRevision=25', git('show', f'HEAD:{version.LEDGER}'))
                self.assertEqual('1.2.5', version.personal(version.current_revision()))
                # Replaying an already versioned commit must not bump twice.
                (root / '.git/rebase-merge').mkdir()
                version.prepare()
                self.assertEqual(25, version.current_revision())

    def test_real_hook_includes_increment_in_the_commit_tree(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            def git(*args):
                return subprocess.check_output(['git', *args], cwd=root, text=True, stderr=subprocess.DEVNULL).strip()
            git('init', '-b', 'personal')
            git('config', 'user.name', 'Version test')
            git('config', 'user.email', 'test@example.invalid')
            (root / version.LEDGER).write_text('personalRevision=24\nreleaseClassification=release\n')
            (root / 'code').write_text('one')
            git('add', '.'); git('commit', '-m', 'base')
            (root / 'scripts').mkdir()
            shutil.copy(Path(__file__).with_name('universe_version.py'), root / 'scripts/universe_version.py')
            hook = root / '.git/hooks/pre-commit'
            hook.write_text('#!/bin/sh\n"' + sys.executable.replace('\\', '/') + '" scripts/universe_version.py prepare\n', encoding='utf-8')
            hook.chmod(0o755)
            (root / 'code').write_text('two')
            git('add', 'code'); git('commit', '-m', 'personal change')
            self.assertIn('personalRevision=25', git('show', f'HEAD:{version.LEDGER}'))
            self.assertEqual('', git('status', '--porcelain', '--', version.LEDGER))

    def test_decrease_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / version.LEDGER).write_text('personalRevision=23\n')
            with patch.object(version, 'ROOT', root), patch.object(version, 'git', return_value='personalRevision=24'):
                with self.assertRaises(SystemExit):
                    version.check('HEAD^')

if __name__ == '__main__':
    unittest.main()
