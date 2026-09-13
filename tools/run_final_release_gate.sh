#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

python tools/validate_phase1_10_final.py
python tools/validate_toolchain_baseline.py

./gradlew --no-daemon \
  :app:compileDebugKotlin \
  :core-model:test \
  :persistence:testDebugUnitTest \
  :persistence:compileDebugAndroidTestKotlin \
  :app:testDebugUnitTest \
  :app:compileDebugAndroidTestKotlin \
  :app:lintDebug

if [[ "${1:-}" == "--device" ]]; then
  ./gradlew --no-daemon :app:connectedDebugAndroidTest
else
  printf '%s\n' "Device tests were not requested. Run again with --device after connecting the acceptance device."
fi

printf '%s\n' "Automated gate completed. Finish docs/REAL_DEVICE_ACCEPTANCE.md before distribution."
