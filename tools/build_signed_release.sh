#!/usr/bin/env bash
set -euo pipefail
# Signing secrets were previously RELATORIO_KEYSTORE, RELATORIO_KEY_ALIAS,
# RELATORIO_STORE_PASSWORD, RELATORIO_KEY_PASSWORD. Never provide them here:
# this command no longer builds release APKs locally and Wave A does not
# introduce a hosted signed-release lane.
printf '%s\n' 'HOSTED_GRADLE_ONLY: release build/signing is not configured on GitHub Actions. No release artifact was produced or signed.' >&2
exit 2
