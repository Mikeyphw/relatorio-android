#!/usr/bin/env python3
"""Wave-A local fail-closed Devtool wrapper/CI policy regression contract.

Offline source policy check only. Does not establish remote build success or
prevent a user from directly invoking a Gradle executable or native Devtool runner.
"""
from pathlib import Path
import re
import tomllib

ROOT = Path(__file__).resolve().parents[1]
CONFIG = tomllib.loads((ROOT / '.devtool.toml').read_text(encoding='utf-8'))
TARGET = 'relatorio_android'
BRANCH = 'master'

# Regression: Android kind + auto mode can silently promote to chroot in Termux
# when chrootctl is installed. Never allow that for source-only wrapper workflows.


def require(ok, message):
    if not ok:
        raise RuntimeError(message)

require(CONFIG['execution']['environment'] == 'host', 'project execution must be host, not auto/chroot')
target = CONFIG['targets'][TARGET]
require(target['execution_environment'] == 'host', 'Android target must stay on invoking host')
require(target['android']['prefer_native_termux'] is True,
        'Android target could auto-promote to chroot on Termux')
for job_name in ('ci-actions-contract', 'ci-source-contract', 'ci-wrapper-contract'):
    require(target['jobs'][job_name]['execution_environment'] == 'host',
            f'job could inherit chroot destination: {job_name}')

wrapper = CONFIG['wrapper']
require(wrapper['schema'] == 2 and wrapper['default_command'] == 'ci-source', 'unsafe wrapper default')
commands = wrapper['commands']
for forbidden in ('build', 'test', 'lint', 'package', 'clean'):
    require(forbidden not in commands, f'local Gradle-capable wrapper entry remains: {forbidden}')
for name, config in commands.items():
    require(config.get('allow_passthrough') is not True, f'unsafe wrapper passthrough: {name}')
    if 'actions' in config:
        require(config['devtool'] == 'actions', f'non-native remote action: {name}')
        require(name.startswith('remote-'), 'remote action incorrectly named')
    else:
        require(config.get('workflow') in ('ci-source', 'ci-local', 'ci-preflight'),
                f'non-source workflow exposed in wrapper: {name}')
        require(config.get('target') == TARGET, f'wrong wrapper target: {name}')

for name, op in (('remote-debug-plan','plan'), ('remote-debug-build','dispatch'),
                 ('remote-watch','watch'), ('remote-resume','resume'), ('remote-artifacts','artifacts')):
    require(commands[name]['actions']['operation'] == op, f'wrong action: {name}')
for name in ('remote-debug-plan','remote-debug-build'):
    spec=commands[name]['actions']
    require(spec['workflow_id'] == 'devtool-android-build.yml' and spec['ref'] == BRANCH,
            f'wrong workflow/ref: {name}')
    require(spec['inputs'] == dict(project_dir='.',module='app',variant='debug',format='apk'),
            f'changed fixed build inputs: {name}')
require(commands['remote-debug-build']['options']['yes'] == dict(
    type='boolean', description='Explicitly authorize GitHub workflow dispatch',
    command_argv=['--yes']), 'dispatch no longer requires explicit yes')
for name in ('remote-watch','remote-resume','remote-artifacts'):
    request=commands[name]['options']['request_id']
    require(request['type'] == 'string' and request['required'] is True and
            request['aliases'] == ['request-id'] and
            request['command_argv'] == ['--request-id','{value}'],
            f'exact request ID binding missing: {name}')
for name in ('ci-source','ci-local','ci-preflight'):
    jobs=CONFIG['targets'][TARGET]['workflows'][name]
    require(jobs and all(isinstance(j,dict) and j['ref'].startswith('job:') for j in jobs),
            f'non-Python/native Gradle runner in source-only workflow: {name}')
for name in ('ci-source', 'ci-local'):
    refs=[j['ref'] for j in CONFIG['targets'][TARGET]['workflows'][name]]
    require('job:ci-wrapper-contract' in refs, f'wrapper contract not in source DAG: {name}')

# Scripts remain as explicit refusals until hosted release lane exists.
for file in ('build-android-app.sh', 'tools/build_signed_release.sh'):
    body=(ROOT/file).read_text(encoding='utf-8')
    require('HOSTED_GRADLE_ONLY' in body and 'exit 2' in body, f'legacy local build not disabled: {file}')
    require(not re.search(r'(?m)^\s*(?:\./)?gradlew\b', body), f'local Gradle remains: {file}')
print('PASS_WAVE_A_HOSTED_ONLY_WRAPPER_SOURCE_CONTRACT')
