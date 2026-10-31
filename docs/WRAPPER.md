# Devtool wrapper — relatorio

See [Devtool orchestration](ci/DEVTOOL_ORCHESTRATION.md) for complete source-only and GitHub Actions usage.

**Policy:** `./devtoolw` defaults to `ci-source`; all Gradle tasks execute only in GitHub Actions. `./devtoolw remote-debug-plan` is read-only; `./devtoolw remote-debug-build --yes` is the explicit mutating dispatch; `./devtoolw remote-resume --request-id REQUEST_ID` is the safe retry path.

Run `./devtoolw --help`, `devtool wrapper describe`, `devtool wrapper resolve ci-source`, and `devtool wrapper verify` to inspect the contract.

**Execution:** All source-check jobs run on the invoking host (`host`), not
inside the `androidos` chroot. The actual project, target and job-level `execution_environment = "host"`
settings prevent implicit Termux-to-chroot dispatch; no unsupported
`no_auto_chroot` configuration key is required.
