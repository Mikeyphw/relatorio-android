#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []

required = [
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/MainViewModel.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/data/UserPreferencesStore.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/RelatorioApp.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/ReviewScreen.kt",
    "core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportReview.kt",
    "core-model/src/test/kotlin/com/mikeyphw/relatorio/model/ReportReviewEngineTest.kt",
    "persistence/src/androidTest/kotlin/com/mikeyphw/relatorio/persistence/ReportRepositoryPersistenceTest.kt",
    "docs/PHASE_5_REVIEW_AND_COMPLETION.md",
]
for relative in required:
    if not (ROOT / relative).is_file():
        errors.append(f"missing: {relative}")

if errors:
    print("Phase 5 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

view_model = (ROOT / required[0]).read_text(encoding="utf-8")
preferences = (ROOT / required[1]).read_text(encoding="utf-8")
app_ui = (ROOT / required[2]).read_text(encoding="utf-8")
review_ui = (ROOT / required[3]).read_text(encoding="utf-8")
review_model = (ROOT / required[4]).read_text(encoding="utf-8")
review_test = (ROOT / required[5]).read_text(encoding="utf-8")
persistence_test = (ROOT / required[6]).read_text(encoding="utf-8")
database = (ROOT / "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDatabase.kt").read_text(encoding="utf-8")
app_build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()

checks = {
    "phase version": ((re.fullmatch(r"0\.[5-9]\.0-phase[5-9]", version) is not None or version == "0.8.0-phase7-8") or version == "1.0.0") and f'versionName = "{version}"' in app_build,
    "review route": "Review," in view_model and "AppRoute.Review" in app_ui,
    "review actions": all(token in view_model for token in ["openReview", "completeReport"]),
    "review screen wired": "ReviewScreen(" in app_ui and "Revisar e concluir" in app_ui,
    "blocking severity": "ReportReviewSeverity.Blocking" in review_model,
    "warning severity": "ReportReviewSeverity.Warning" in review_model,
    "sunday school validation": "consistency.sunday_school" in review_model,
    "communion validation": "consistency.communion" in review_model,
    "movement month validation": "date_outside_month" in review_model,
    "duplicate report warning": "duplicate.completed_report" in view_model,
    "numeric progress gate": "progress.numeric_sections" in view_model,
    "completion confirmation": "Concluir este relatório?" in review_ui,
    "completion button gate": "enabled = canComplete" in review_ui,
    "repository completion": "repository.complete(draft.id)" in view_model,
    "progress cleanup": "clearDraftProgress" in preferences and "preferences.clearDraftProgress()" in view_model,
    "review unit tests": "ReportReviewEngineTest" in review_test,
    "completion persistence test": "completingReportMovesItOutOfDrafts" in persistence_test,
    "no Room migration": re.search(r"version\s*=\s*1", database) is not None,
    "offline manifest": "android.permission.INTERNET" not in (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8"),
}
for name, passed in checks.items():
    if not passed:
        errors.append(f"failed check: {name}")

for file in ROOT.rglob("*.kt"):
    text = file.read_text(encoding="utf-8")
    if "\t" in text:
        errors.append(f"tab character: {file.relative_to(ROOT)}")
    if "import androidx.compose.foundation.layout.weight" in text:
        errors.append(f"invalid weight import: {file.relative_to(ROOT)}")

if errors:
    print("Phase 5 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Phase 5 validation passed")
print("- pure review engine with blocking issues and warnings: present")
print("- numeric progress and duplicate-report checks: present")
print("- final review screen and explicit completion confirmation: present")
print("- Room completion and DataStore progress cleanup: present")
print("- Room schema remains version 1")
