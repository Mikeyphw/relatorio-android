#!/usr/bin/env bash
set -euo pipefail

: "${RELATORIO_KEYSTORE:?Defina RELATORIO_KEYSTORE com o caminho da chave.}"
: "${RELATORIO_KEY_ALIAS:?Defina RELATORIO_KEY_ALIAS.}"
: "${RELATORIO_STORE_PASSWORD:?Defina RELATORIO_STORE_PASSWORD.}"
: "${RELATORIO_KEY_PASSWORD:?Defina RELATORIO_KEY_PASSWORD.}"
: "${ANDROID_HOME:?Defina ANDROID_HOME.}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

./gradlew :app:assembleRelease

UNSIGNED="$ROOT/app/build/outputs/apk/release/app-release-unsigned.apk"
SIGNED="$ROOT/app/build/outputs/apk/release/relatorio-mensal-ad-1.0.0-signed.apk"
[[ -f "$UNSIGNED" ]] || { echo "APK release não encontrado: $UNSIGNED" >&2; exit 1; }

BUILD_TOOLS="$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
APKSIGNER="$BUILD_TOOLS/apksigner"
[[ -x "$APKSIGNER" ]] || { echo "apksigner não encontrado." >&2; exit 1; }

rm -f "$SIGNED"
"$APKSIGNER" sign \
  --ks "$RELATORIO_KEYSTORE" \
  --ks-key-alias "$RELATORIO_KEY_ALIAS" \
  --ks-pass "env:RELATORIO_STORE_PASSWORD" \
  --key-pass "env:RELATORIO_KEY_PASSWORD" \
  --out "$SIGNED" \
  "$UNSIGNED"

"$APKSIGNER" verify --verbose --print-certs "$SIGNED"
echo "APK assinado: $SIGNED"
