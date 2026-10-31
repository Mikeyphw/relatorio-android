# Relatório Mensal AD — GitHub Actions through Devtool

This adds **debug-only GitHub CI and manual Devtool dispatch**, while retaining Android target metadata; local Gradle and local release signing are blocked under the hosted-only policy.

## Contents

- `.github/workflows/relatorio-android-ci.yml`: push / PR / manual CI. Runs core-model, persistence and app JVM tests; compiles (but **does not execute**) instrumented Android tests; runs app debug lint; builds, verifies and uploads a debug APK. Publishes test/lint reports when available.
- `.github/workflows/devtool-android-build.yml`: manual `workflow_dispatch` with the four Android-build input keys; admits **only** project `.`, module `app`, variant `debug`, format `apk`. Applies the same checks and uploads a SHA-256/source-bound JSON evidence receipt beside a single verified APK.
- `scripts/verify_relatorio_actions.py`: offline static contract and GitHub-run APK receipt generator.

These workflows run on `ubuntu-24.04`, Java 17, Android SDK 36, Build Tools 36.0.0, and Gradle 9.4.1 via the checked-in checksum-pinned wrapper. The existing AGP 9.2.1, Kotlin 2.3.20, Room and KSP configuration is not modified.

## Installation and commit

Use the **bundle's** `apply.sh`, not just a direct patch copy; it checks the exact uploaded repository snapshot and refuses dirty / incompatible trees. Review the resulting changes:

```bash
cd ~/Code/relatorio-android
python3 scripts/verify_relatorio_actions.py --static
git diff --stat && git status --short
git add .github/workflows/devtool-android-build.yml \
  .github/workflows/relatorio-android-ci.yml \
  scripts/verify_relatorio_actions.py docs/ci/DEVTOOL_GITHUB_ACTIONS.md
git commit -m 'Add source-bound Devtool GitHub Actions Android CI'
git push
```

If the directory is different, set `RELATORIO_ANDROID_REPO=/path/to/git-root` when applying the bundle and substitute that directory in commands below.

## GitHub setup

- Enable Actions in your GitHub repository. No signing secrets are required for debug builds.
- Authenticate locally using `gh auth login` or `GH_TOKEN`/`GITHUB_TOKEN` with repository Actions privileges.
- Use the branch that **contains the committed workflow** and is pushed to GitHub. GitHub Actions `workflow_dispatch` registration might also require the workflow to exist on the default branch; merge it first if GitHub reports it unavailable.
- Devtool auto-detects `OWNER/REPO` from the Git `origin`. Override with `--github-repo OWNER/REPO` if needed.

```bash
cd ~/Code/relatorio-android
REF="$(git branch --show-current)"
# Read-only; no network mutation:
devtool --no-hud -r "$PWD" actions diagnose --json
# This project's multiline `include(...)` block is not recognized by
# Devtool's Android-specific static module discovery in this snapshot.
# Use the fully supported *generic* Actions provider to dispatch this
# restricted Android workflow without changing existing Gradle semantics.
devtool --no-hud -r "$PWD" actions plan devtool-android-build.yml \
  --ref "$REF" \
  --input project_dir=. --input module=app \
  --input variant=debug --input format=apk --json
# Explicit remote dispatch:
devtool --no-hud -r "$PWD" actions run devtool-android-build.yml \
  --ref "$REF" \
  --input project_dir=. --input module=app \
  --input variant=debug --input format=apk --yes --json
```

Keep the `run_id` (or `request_id` if GitHub did not return a correlated run). Never assume an accepted dispatch means the build passed; do **not** blindly dispatch again after a network interruption.

```bash
devtool --no-hud -r "$PWD" actions watch --run-id RUN_ID --interval 5 --max-checks 120 --json
devtool --no-hud -r "$PWD" actions artifacts --run-id RUN_ID --json
# Choose the artifact ID named relatorio-android-debug-apk-<run>-<attempt>:
devtool --no-hud -r "$PWD" actions android collect \
  --run-id RUN_ID --artifact-id ARTIFACT_ID --format apk --variant debug \
  --expected-package com.mikeyphw.relatorio.android.debug \
  --download --json
# For a network-ambiguous dispatch:
devtool --no-hud -r "$PWD" actions resume --request-id REQUEST_ID --json
```

`android collect` needs locally available `apksigner` and `aapt`/`aapt2`. If it rejects ambiguous member selection, set `--member relatorio-mensal-ad-debug.apk` (the artifact ZIP includes evidence JSON as well). See `devtool actions android collect --help` for installed version behavior.

To manually trigger full CI from Devtool rather than using push/PR hooks:

```bash
devtool --no-hud -r "$PWD" actions plan relatorio-android-ci.yml --ref "$REF" --json
devtool --no-hud -r "$PWD" actions run relatorio-android-ci.yml --ref "$REF" --yes --json
```

## Qualification boundary

- Source-bound overlay validation and Gradle compile/tests/lint are different evidence layers.
- JVM tests **do not replace** real Android device tests for PDF output, Room runtime, accessibility, storage and backup behavior.
- AndroidTest Kotlin test **compilation** does not execute on an emulator or physical device.
- APK has debug signing and **cannot** be represented as the officially signed production release.
- The legacy `tools/build_signed_release.sh` now fails closed; hosted release/signing is not part of Wave A. No signing secrets, keystore material, APKs, or release attestations are committed.
- GitHub Actions success is a remote build signal. It is not automatically Devtool's `device_qualified` or `release_qualified` verdict.

### Expected errors

`actions plan`: ensure local HEAD is clean, committed, pushed, and the workflow exists remotely at the chosen ref. The Android-specific `actions android discover/plan/run` commands cannot currently see `:app` with this project's multiline `include(...)`; use generic `actions plan/run` as shown above. This avoids modifying Gradle solely for Devtool parser compatibility. `actions collect`: check `apksigner`/`aapt`, run success and correct artifact ID. CI build failures: inspect run logs; Java/SDK/AGP dependency resolution is first verified on the GitHub runner rather than claimed in the offline overlay test.
