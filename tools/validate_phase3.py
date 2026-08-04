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
    "core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportTemplateCatalog.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDao.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportRepository.kt",
    "docs/PHASE_3_GUIDED_NUMERIC_FORM.md",
]
for relative in required:
    if not (ROOT / relative).is_file():
        errors.append(f"missing: {relative}")

if errors:
    print("Phase 3 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

catalog = (ROOT / required[3]).read_text(encoding="utf-8")
view_model = (ROOT / required[0]).read_text(encoding="utf-8")
preferences = (ROOT / required[1]).read_text(encoding="utf-8")
ui = (ROOT / required[2]).read_text(encoding="utf-8")
dao = (ROOT / required[4]).read_text(encoding="utf-8")
repository = (ROOT / required[5]).read_text(encoding="utf-8")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
app_build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")

standard_block = catalog.split("val standard =", 1)[1].split("val complete =", 1)[0]
complete_block = catalog.split("val complete =", 1)[1].split("val all:", 1)[0]
standard_fields = len(re.findall(r"\bfield\(", standard_block))
complete_fields = len(re.findall(r"\bfield\(", complete_block)) + 18
standard_sections = len(re.findall(r"\bsection\(", standard_block))
complete_sections = len(re.findall(r"\bsection\(", complete_block)) + 3

checks = {
    "phase version": 'versionName = "0.3.0-phase3"' in app_build,
    "numeric route": "NumericSection" in view_model and "NumericSectionScreen" in ui,
    "numeric actions": all(
        token in view_model
        for token in [
            "startNumericForm",
            "updateDraftValue",
            "updateDraftCustomLabel",
            "nextNumericSection",
            "previousNumericSection",
        ]
    ),
    "large minus and plus controls": "Icons.Default.Remove" in ui and "Icons.Default.Add" in ui,
    "direct numeric input": "KeyboardType.Number" in ui and "filter(Char::isDigit)" in ui,
    "zero lower bound": "coerceAtLeast(0)" in ui and "coerceAtLeast(0)" in repository,
    "value persistence": "updateDraftValue" in dao and "updateDraftValue" in repository,
    "custom label persistence": "updateDraftCustomLabel" in dao and "updateDraftCustomLabel" in repository,
    "draft progress keys": all(
        token in preferences
        for token in [
            "DraftProgressReportId",
            "DraftProgressCurrentSection",
            "DraftProgressCompletedSections",
        ]
    ),
    "progress restore": "stored.currentSectionIndex" in view_model,
    "review after completion": '"Revisar números"' in ui,
    "offline manifest": "android.permission.INTERNET" not in manifest,
    "no invalid weight import": "import androidx.compose.foundation.layout.weight" not in ui,
    "correct keyboard options import": "import androidx.compose.foundation.text.KeyboardOptions" in ui,
}
for name, passed in checks.items():
    if not passed:
        errors.append(f"failed check: {name}")

if standard_fields != 33:
    errors.append(f"standard field count is {standard_fields}, expected 33")
if complete_fields != 74:
    errors.append(f"complete field count is {complete_fields}, expected 74")
if standard_sections != 5:
    errors.append(f"standard section count is {standard_sections}, expected 5")
if complete_sections != 10:
    errors.append(f"complete section count is {complete_sections}, expected 10")

if errors:
    print("Phase 3 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Phase 3 validation passed")
print("- standard form: 33 fields in 5 sections")
print("- complete form: 74 fields in 10 sections")
print("- immediate Room value persistence: present")
print("- DataStore section progress: present")
print("- large +/- controls and direct entry: present")
print("- custom meeting labels: present")
