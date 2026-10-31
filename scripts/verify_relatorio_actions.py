#!/usr/bin/env python3
"""Offline Devtool/GitHub workflow contract; GitHub runner-only debug APK evidence.

Does not sign releases, contact GitHub or install on devices.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_APP = 'com.mikeyphw.relatorio.android'
EXPECTED_DEBUG = EXPECTED_APP + '.debug'
EXPECTED_APK = 'relatorio-mensal-ad-debug.apk'


def check(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def source(path: str) -> str:
    file = ROOT / path
    check(file.is_file() and not file.is_symlink(), f'unsafe or missing source: {path}')
    return file.read_text(encoding='utf-8')


def static() -> None:
    import tomllib

    config = tomllib.loads(source('.devtool.toml'))
    check('relatorio_android' in config.get('targets', {}), 'Devtool Android target missing')
    check(config['targets']['relatorio_android'].get('runner') == 'android', 'Devtool runner changed')
    app = source('app/build.gradle.kts')
    for needle in (
        'alias(libs.plugins.android.application)',
        'compileSdk = 36', 'targetSdk = 36', 'minSdk = 26',
        f'applicationId = "{EXPECTED_APP}"',
        'applicationIdSuffix = ".debug"',
        'versionCode = 10', 'versionName = "1.0.0"',
        'testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"',
    ):
        check(needle in app, f'Android Gradle contract changed: {needle}')
    check(source('VERSION').strip() == '1.0.0', 'unsupported VERSION change')
    gradle_version = source('gradle/wrapper/gradle-wrapper.properties')
    check('gradle-9.4.1-bin.zip' in gradle_version and 'distributionSha256Sum=' in gradle_version,
          'missing checksum-pinned Gradle 9.4.1 wrapper')
    jar = ROOT / 'gradle/wrapper/gradle-wrapper.jar'
    check(jar.is_file() and not jar.is_symlink() and jar.stat().st_size > 0,
          'missing Gradle wrapper JAR')
    check('org.gradle.wrapper.GradleWrapperMain' in source('gradlew'), 'unexpected Gradle entry point')
    check('include(' in source('settings.gradle.kts') and all(
        f'":{m}"' in source('settings.gradle.kts') for m in ('app','core-model','persistence')
    ), 'Gradle module membership changed')
    check('android.permission.INTERNET' not in source('app/src/main/AndroidManifest.xml'),
          'offline app permission contract changed')
    check((ROOT / 'tools/validate_phase9_10.py').is_file(), 'current phase validator missing')

    from verify_wave_c import check as validate_wave_c
    validate_wave_c(ROOT)
    print('Relatorio Android Devtool/GitHub Actions contracts: PASS (offline)')


def command(argv: list[str]) -> str:
    result = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            text=True, check=False, timeout=30)
    check(result.returncode == 0, f'Android verification command failed: {Path(argv[0]).name}')
    return result.stdout + '\n' + result.stderr


def evidence(apk: Path, dest: Path) -> None:
    event = os.environ.get('GITHUB_EVENT_NAME', '')
    check(os.environ.get('GITHUB_ACTIONS') == 'true' and
          event in ('workflow_dispatch','push','pull_request'),
          'APK receipt requires supported GitHub Actions job')
    check(bool(os.environ.get('GITHUB_WORKFLOW')), 'missing GitHub workflow name')
    sha = os.environ.get('GITHUB_SHA', '')
    run_id = os.environ.get('GITHUB_RUN_ID', '')
    attempt = os.environ.get('GITHUB_RUN_ATTEMPT', '')
    slug = os.environ.get('GITHUB_REPOSITORY', '')
    check(re.fullmatch(r'[0-9a-f]{40}', sha) is not None, 'invalid GitHub source SHA')
    check(run_id.isdecimal() and attempt.isdecimal() and int(run_id)>0 and int(attempt)>0,
          'invalid GitHub run identification')
    check(re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', slug) is not None,
          'missing GitHub repository identity')
    check(command(['git', '-C', str(ROOT), 'rev-parse', 'HEAD']).strip() == sha,
          'actual checked-out source differs from dispatched source')
    check(apk.is_file() and not apk.is_symlink() and apk.name == EXPECTED_APK and apk.stat().st_size > 0,
          'unexpected or missing APK')
    check(not dest.exists() and not dest.is_symlink(), 'refusing to overwrite existing evidence')
    sdk = Path(os.environ.get('ANDROID_HOME', '/nonexistent')) / 'build-tools/36.0.0'
    signer = command([str(sdk / 'apksigner'), 'verify', '--print-certs', str(apk)])
    match = re.findall(r'(?im)^Signer #1 certificate SHA-256 digest:\s*([0-9a-f]{64})\s*$', signer)
    check(len(match) == 1, 'APK signing fingerprint missing or ambiguous')
    package = command([str(sdk / 'aapt'), 'dump', 'badging', str(apk)])
    check(re.search(r"(?m)^package: name='" + re.escape(EXPECTED_DEBUG) + r"'\s", package) is not None,
          'unexpected APK application ID')
    with apk.open('rb') as fh:
        digest = hashlib.file_digest(fh, 'sha256').hexdigest()
    receipt = {
        'schema_version': 2,
        'producer': 'relatorio-android-devtool-github-actions',
        'source_sha': sha, 'repository': slug,
        'github_run_id': int(run_id), 'github_run_attempt': int(attempt),
        'run_id': int(run_id), 'run_attempt': int(attempt),
        'workflow_event': event, 'event': event,
        'workflow_name': os.environ['GITHUB_WORKFLOW'],
        'workflow_run_url': f'https://github.com/{slug}/actions/runs/{run_id}', 'module': 'app', 'variant': 'debug',
        'format': 'apk', 'package': EXPECTED_DEBUG,
        'artifact_member': EXPECTED_APK, 'apk_size_bytes': apk.stat().st_size,
        'apk_sha256': digest, 'signing_certificate_sha256': match[0].lower(),
        'apk_signature_verified': True, 'apk_manifest_verified': True,
        'unit_test_and_lint_workflow_prerequisites': True,
        'unit_tests_and_lint_passed': True,
        'device_tests_executed': False, 'device_qualified': False,
        'signed_release': False, 'release_qualified': False, 'qualified_release': False,
    }
    with dest.open('x', encoding='utf-8') as out:
        json.dump(receipt, out, indent=2, sort_keys=True)
        out.write('\n')
    print(f'Debug APK source-bound evidence: {dest}')


def main() -> int:
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--static', action='store_true')
    mode.add_argument('--evidence', nargs=2, metavar=('APK', 'OUTPUT_JSON'))
    args = parser.parse_args()
    try:
        if args.static:
            static()
        else:
            static()
            evidence(Path(args.evidence[0]), Path(args.evidence[1]))
    except (ValueError, OSError, subprocess.TimeoutExpired) as exc:
        print(f'Relatorio Android Actions: FAIL: {exc}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
