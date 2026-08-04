#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./gradlew \
  --no-daemon \
  --max-workers=1 \
  :core-model:test \
  :persistence:testDebugUnitTest \
  :app:testDebugUnitTest \
  :app:assembleDebug
printf '\nAPK: %s\n' "$PWD/app/build/outputs/apk/debug/app-debug.apk"
