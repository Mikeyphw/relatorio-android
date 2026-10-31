# Devtool Wave A — relatorio

The checked-in `devtoolw` launcher remains generated and unchanged. Native
Devtool wrapper schema 2 is the canonical entrypoint for local source evidence
and typed GitHub Actions dispatch. **Every Gradle command belongs in GitHub
Actions**, including builds, lint and tests; no local Gradle is run by the
source-only workflow or the safe wrapper commands.

## Execution destination: Termux host (Wave A follow-up)

The source-only workflows are pinned to `execution.environment = "host"`,
`targets.relatorio_android.execution_environment = "host"`, and per-job
`execution_environment = "host"`. Explicit host routing prevents Devtool's
Android auto policy from choosing the chroot when `chrootctl` is available.
The target also sets
`android.prefer_native_termux = true`. These settings are about lightweight
Python validation only; **Gradle never runs locally through this wrapper**.
An explicit override could still request a different execution destination.
Use `devtool -r "$PWD" config resolve` and `devtool -r "$PWD" preflight
--workflow ci-source --target relatorio_android` to check the effective policy.

## Everyday commands

```sh
./devtoolw                         # ci-source by default; no Gradle
./devtoolw ci-source               # source and wrapper contract checks
./devtoolw ci-local                # retained alias: source-only checks
./devtoolw ci-preflight            # workflow/source preflight only
./devtoolw validate                # source-only alias, NOT Android runner validate
python3 scripts/verify_devtool_wave_a.py

./devtoolw remote-debug-plan       # read-only Actions request plan
# Commit/push a clean source HEAD to master; inspect the plan before dispatch.
./devtoolw remote-debug-build --yes # mutating remote dispatch; no local Gradle
# From returned 32-hex request_id:
./devtoolw remote-watch --request-id REQUEST_ID
./devtoolw remote-resume --request-id REQUEST_ID
./devtoolw remote-artifacts --request-id REQUEST_ID
```

The typed remote operations use `devtool-android-build.yml` with fixed inputs
`project_dir=.`, `module=app`, `variant=debug`, `format=apk`, and ref `master`.
Only the build shortcut dispatches, and it requires explicit `--yes`.
Devtool resolves existing same-request dispatches safely; after an uncertain
network result **resume the request, never blindly dispatch again**.
To target a different pushed branch, use native `devtool actions plan/run`
with `--ref BRANCH`; the wrapper shortcut intentionally pins `master`.

## Limits

- Local source checks don't run JVM tests or Android lint: those run in GitHub CI.
- GitHub workflow success does not by itself qualify an APK, device or release.
- Direct `./gradlew`, native `devtool build/test/validate`, and externally invoked
  Gradle commands remain possible. Wrapper gating alone **does not enforce an
  upstream Gradle-only-on-GitHub policy**; that belongs to later Devtool core work.
- `remote-artifacts` lists remote evidence. APK collection, cryptographic
  verification, and device qualification are separate explicit operations.
- A default-branch shortcut still requires a real GitHub auth context, matching
  repo/ref and a clean, committed, pushed HEAD. Offline testing never dispatches.

## Native diagnostic commands

```sh
devtool --repo "$PWD" wrapper describe
devtool --repo "$PWD" wrapper resolve ci-source
devtool --repo "$PWD" plan ci-source --target relatorio_android --json
devtool --repo "$PWD" preflight --workflow ci-source --target relatorio_android
devtool --repo "$PWD" runs --help
devtool --repo "$PWD" actions diagnose --json
```

## Wave A acceptance

`ci-source`, `ci-local`, and `ci-preflight` admit only named local source jobs.
`remote-debug-plan` and all request-id commands resolve without network calls;
`remote-debug-build --yes` is the sole wrapper mutating dispatch command.
The embedded overlay validator and `scripts/verify_devtool_wave_a.py` check
this policy locally without Gradle or GitHub network access.

`build-android-app.sh` now refuses local Gradle. The old signing script also
fails closed: **Wave A does not add a GitHub release-building/signing lane**.
Signing a release requires a separately reviewed hosted release workflow; no
signing keys, APK release or release qualification are created here.
`settings.gradle.kts` keeps its valid trailing comma; Devtool's upstream parser
limitation is deferred to Wave B. The generic typed Actions dispatcher works.


## Wave B: source-only DAG and affected ownership

`./devtoolw ci-source` remains the default and only calls Python source-check jobs.
The target now explicitly owns workflow YAML, source directories, validation
scripts and documentation. `devtool affected --explain` may be used to inspect
conservative target selection; it is not a substitute for CI artifact evidence.
All source jobs run on `host`, including Termux; Android Gradle dispatch stays
exclusively within GitHub Actions.

Only stable phase 2, phase 3 and phase 9/10 source validators were integrated. Legacy phase 4–8 validators currently reject the modern version and were intentionally not wired into the default DAG.
