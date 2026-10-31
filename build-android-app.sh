#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' 'HOSTED_GRADLE_ONLY: local APK build disabled. Run ./devtoolw remote-debug-plan, then ./devtoolw remote-debug-build --yes after committing and pushing.' >&2
exit 2
