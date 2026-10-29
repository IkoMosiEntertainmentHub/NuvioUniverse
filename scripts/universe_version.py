"""Independent personal version ledger and repository-local commit guard (Python 3)."""
import argparse
import os
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LEDGER = 'universe-version.properties'

def git(*args, cwd=None):
    return subprocess.check_output(['git', *args], cwd=cwd or ROOT, text=True).strip()

def properties(text):
    return dict(line.split('=', 1) for line in text.splitlines() if '=' in line and not line.startswith('#'))

def personal(revision):
    if revision < 0:
        raise ValueError('Personal revision cannot decrease below zero')
    total = 100 + revision
    return f'{total // 100}.{total // 10 % 10}.{total % 10}'

def identifier(official, build, revision):
    return f'NuvioUniverse-{official}-({build})-v{personal(revision)}'

def current_revision():
    return int(properties((ROOT / LEDGER).read_text())['personalRevision'])

def bump():
    revision = current_revision() + 1
    text = (ROOT / LEDGER).read_text()
    (ROOT / LEDGER).write_text(re.sub(r'(?m)^personalRevision=\d+$', f'personalRevision={revision}', text))
    return revision

def qualifies(paths):
    # Each personally authored content change qualifies; the ledger itself does not.
    return any(p and p != LEDGER for p in paths)

def prepare(amend=False):
    if os.environ.get('NUVIO_OFFICIAL_SYNC') == '1':
        return
    # Rebase/cherry-pick replay must preserve the already assigned revision.
    for marker in ('rebase-merge', 'rebase-apply', 'CHERRY_PICK_HEAD'):
        if (ROOT / git('rev-parse', '--git-path', marker)).exists():
            return
    merge = ROOT / git('rev-parse', '--git-path', 'MERGE_HEAD')
    if merge.exists():
        # An official-only synchronization does not increment this independent ledger.
        official = subprocess.run(['git', 'merge-base', '--is-ancestor', merge.read_text().strip(), 'upstream/cmp-rewrite'], cwd=ROOT)
        if official.returncode == 0:
            return
    paths = git('diff', '--cached', '--name-only').splitlines()
    if not qualifies(paths):
        return
    previous = properties(git('show', f'{"HEAD^" if amend else "HEAD"}:{LEDGER}'))
    before = int(previous.get('personalRevision', 0))
    revision = current_revision()
    if revision < before:
        raise SystemExit('Personal revision must never decrease')
    if revision == before:
        bump()
        subprocess.check_call(['git', 'add', LEDGER], cwd=ROOT)
    elif revision != before + 1:
        raise SystemExit('Each personal change commit increments exactly one revision')

def check(base):
    revision = current_revision()
    old = properties(git('show', f'{base}:{LEDGER}'))
    previous = int(old.get('personalRevision', 0))
    if revision < previous:
        raise SystemExit('Personal revision decreased')
    parents = git('rev-list', '--parents', '-n', '1', 'HEAD').split()[1:]
    if len(parents) == 1:
        changed = git('diff', '--name-only', base, 'HEAD').splitlines()
        if qualifies(changed) and revision != previous + 1:
            raise SystemExit('A personal change commit must advance exactly one revision')
    elif parents:
        inherited = []
        for parent in parents:
            try:
                inherited.append(int(properties(git('show', f'{parent}:{LEDGER}')).get('personalRevision', 0)))
            except subprocess.CalledProcessError:
                inherited.append(0) # Official upstream has no personal ledger.
        if revision != max(inherited):
            raise SystemExit('A merge must preserve the greatest inherited personal revision')
    # Guard the build wiring after upstream edits as well as the ledger.
    for filename in ('androidApp/build.gradle.kts', 'composeApp/build.gradle.kts'):
        content = (ROOT / filename).read_text()
        if 'personalRevision' not in content or 'NuvioUniverse-' not in content:
            raise SystemExit(f'Independent version composition missing from {filename}')
    print(f'Preserved personal version v{personal(revision)}')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['show', 'bump', 'prepare', 'install-hooks', 'check'])
    parser.add_argument('--base', default='HEAD^')
    parser.add_argument('--amend', action='store_true')
    args = parser.parse_args()
    if args.command == 'bump':
        print(personal(bump()))
    elif args.command == 'prepare':
        prepare(amend=args.amend)
    elif args.command == 'install-hooks':
        subprocess.check_call(['git', 'config', 'core.hooksPath', '.githooks'], cwd=ROOT)
    elif args.command == 'check':
        check(args.base)
    else:
        config = (ROOT / 'iosApp/Configuration/Version.xcconfig').read_text()
        def value(key):
            return re.search(rf'(?m)^\s*{key}\s*=\s*(.+?)\s*$', config)[1]
        official = value('MARKETING_VERSION')
        label = properties((ROOT / 'universe-upstream-label.properties').read_text())
        if label.get('marketingVersion') == official:
            official = label['releaseVersion']
        print(identifier(official, value('CURRENT_PROJECT_VERSION'), current_revision()))

if __name__ == '__main__':
    main()
