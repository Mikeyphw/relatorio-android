#!/usr/bin/env python3
"""Wave B source-only phase/DAG/ownership/host policy regression check."""
from pathlib import Path
import tomllib
root=Path(__file__).resolve().parents[1]
c=tomllib.loads((root/'.devtool.toml').read_text())
t=c['targets']['relatorio_android']
assert c['execution']['environment'] == t['execution_environment'] == 'host'
assert t['android']['prefer_native_termux'] is True
assert {'app/**','core-model/**','persistence/**','tools/**','scripts/**','.github/workflows/**','docs/**'} <= set(t['inputs'])
expected=('ci-phase2-contract','ci-phase3-contract','ci-phase9-10-contract','ci-wave-b-contract')
for name in expected+('ci-actions-contract','ci-source-contract','ci-wrapper-contract'):
    j=t['jobs'][name]
    assert j['runner']=='command' and j['execution_environment']=='host', name
    assert j['metadata']['read_only'] is True and 'outputs' not in j, name
for workflow in ('ci-source','ci-local'):
    refs={n['ref'] for n in t['workflows'][workflow]}
    assert {f'job:{k}' for k in expected} <= refs
for workflow in ('ci-source','ci-local','ci-preflight'):
    assert all(n['ref'].startswith('job:') for n in t['workflows'][workflow])
for n in ('ci-phase2-contract','ci-phase3-contract','ci-phase9-10-contract'):
    cmd=t['jobs'][n]['command']
    assert cmd[0]=='python3' and cmd[1].startswith('tools/validate_phase'), n
print('PASS_WAVE_B_RELATORIO_SOURCE_DAG_HOST_OWNERSHIP')
