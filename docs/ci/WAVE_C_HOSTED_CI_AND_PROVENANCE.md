# Wave C — Hosted build consolidation and evidence contracts

Repository: **Relatorio Android** · 2026-10-30

## Design

- The existing Devtool wrapper's source-only default (`./devtoolw`, `ci-source`) is preserved. Source and evidence policy checks run on the invoking **host**. No Gradle, SDK build, or chroot is executed locally.
- `.github/actions/verified-debug-build/action.yml` is the single hosted implementation for source checks, Java/SDK configuration, Gradle, unit tests, lint, APK signature/manifest verification, receipt creation and artifact upload. Both `devtool-android-build.yml` and the automatic CI workflow call this same action. All Gradle operations run on **GitHub Actions**.
- The manual Devtool workflow retains exactly its four fixed dispatch inputs (`project_dir`, `module`, `variant`, `format`); dispatch is still opt-in and goes through typed Devtool Actions.
- APK artifacts contain two files at the archive root: a verified debug APK and `build-evidence.json`. Schema **2** records the checked-out Git SHA, repository, workflow, GitHub run/attempt, event, artifact member, package, SHA256, signing-certificate SHA256, and explicit negative release/device qualifications.
- `scripts/verify_wave_c.py` and `scripts/test_wave_c.py` guard the shared action's checks, ordering, permissions, source identity, and receipt upload. Mutations deliberately bypassing those checks must fail.
- `scripts/verify_wave_c_artifact.py` independently verifies downloaded ZIP structure, SHA256, receipt fields, and the **user-provided** expected GitHub run and SHA without Gradle. It **does not** cryptographically verify the Android signing certificate, authenticate the GitHub service, or qualify installation/production use. Confirm the run and artifact source on GitHub before trusting a downloaded ZIP.

## Local checks

```bash
./devtoolw ci-source
python3 scripts/test_wave_c.py
```

## Verify downloaded artifact offline

```bash
python3 scripts/verify_wave_c_artifact.py /path/to/downloaded-artifact.zip \
  --source-sha <the-40-hex-checkout-sha> --run-id <github-run-number> --attempt 1
```

These arguments are intentionally required so that an internally consistent but unrelated artifact cannot silently pass as the requested build.

## GitHub execution

The existing `./devtoolw remote-debug-plan`, `remote-debug-build --yes`, `remote-watch`, `remote-resume` and `remote-artifacts` still refer to `devtool-android-build.yml`. Push the updated workflows to the selected branch before planning/dispatching; Devtool checks the workflow as registered on GitHub.

## Opt-in instrumentation lane

`.github/workflows/relatorio-emulator-tests.yml` adds a **manual-only** GitHub-hosted Android 35 emulator lane using `:app:connectedDebugAndroidTest`. Its smoke/PDF tests may require compatibility fixes after first live execution. Merely checking in the workflow does not establish that tests passed. Enable on GitHub using:

```bash
gh workflow run relatorio-emulator-tests.yml --ref master
```

This lane does not run automatically on push/PR and no release quality claim follows from compilation-only AndroidTest tasks.
