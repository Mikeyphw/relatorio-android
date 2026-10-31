# Wave E — historical-phase validation at 1.0.0

Phase 4, 5, 6 and 7–8 validators are active again in the `ci-source` and
`ci-local` host-only DAGs, and in the hosted GitHub source checks. Their feature,
privacy, persistence and regression assertions remain intact. The only relaxed
assertion is historical `VERSION`/Gradle `versionName`: the released app at
`1.0.0` is now accepted *provided both values agree*. The validators keep their
historical prerelease formats for archival snapshots.

The Python validators require no Gradle/Android SDK, root, chroot or network.
`ci-source` is the local default. GitHub builds and emulator tests still require
published source and execution on GitHub Actions; no release is authorized.

The Android application is not yet hosted in an identified GitHub repository.
Do not fabricate a destination or claim its hosted acceptance is complete.
