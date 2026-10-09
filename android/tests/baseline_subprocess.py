"""Run existing source guards without publishing private development Git history.

Only the exact Git source responses used by the tests are stored. Missing or
altered fixtures fail closed. Recording is a local maintainer operation.
"""
from pathlib import Path
import subprocess as _real
import hashlib
import json
import os

_fixtures = Path(__file__).with_name('baselines')


def check_output(command, **kwargs):
    if not command or command[0] != 'git':
        return _real.check_output(command, **kwargs)
    args = list(command[1:])
    if args[:1] == ['-C']:
        args = args[2:]
    if args[:1] not in (['show'], ['ls-tree']):
        raise ValueError('Unreviewed Git baseline operation')
    key = hashlib.sha256(json.dumps(args).encode()).hexdigest()
    data_file = _fixtures / (key + '.source')
    manifest_file = _fixtures / (key + '.json')
    if not data_file.exists():
        recording_repo = os.environ.get('CODEX_METER_BASELINE_REPO')
        if not recording_repo:
            raise FileNotFoundError('Missing audited source baseline: ' + repr(args))
        raw = _real.check_output(['git', '-C', recording_repo, *args])
        _fixtures.mkdir(exist_ok=True)
        data_file.write_bytes(raw)
        manifest_file.write_text(json.dumps({'git_arguments': args,
            'sha256': hashlib.sha256(raw).hexdigest()}, indent=2) + '\n', encoding='utf-8')
    raw = data_file.read_bytes()
    manifest = json.loads(manifest_file.read_text(encoding='utf-8'))
    if manifest['git_arguments'] != args or hashlib.sha256(raw).hexdigest() != manifest['sha256']:
        raise ValueError('Source baseline integrity check failed')
    if kwargs.get('text') or kwargs.get('universal_newlines') or kwargs.get('encoding'):
        return raw.decode(kwargs.get('encoding') or 'utf-8', errors=kwargs.get('errors') or 'strict').replace('\r\n', '\n')
    return raw


def __getattr__(name):
    return getattr(_real, name)
