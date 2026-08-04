#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []

required = [
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/MainActivity.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/MainViewModel.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/data/UserPreferencesStore.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/AccessibilitySettingsScreen.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/PdfExportScreen.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/RelatorioApp.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/Theme.kt",
    "core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportHistory.kt",
    "core-model/src/test/kotlin/com/mikeyphw/relatorio/model/ReportHistoryTest.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDao.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportRepository.kt",
    "docs/PHASE_7_ACCESSIBILITY_USABILITY.md",
    "docs/PHASE_8_HISTORY_MAINTENANCE.md",
]
for relative in required:
    if not (ROOT / relative).is_file():
        errors.append(f"missing: {relative}")

if errors:
    print("Phase 7-8 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

main_activity = (ROOT / required[0]).read_text(encoding="utf-8")
view_model = (ROOT / required[1]).read_text(encoding="utf-8")
preferences = (ROOT / required[2]).read_text(encoding="utf-8")
accessibility_ui = (ROOT / required[3]).read_text(encoding="utf-8")
pdf_ui = (ROOT / required[4]).read_text(encoding="utf-8")
app_ui = (ROOT / required[5]).read_text(encoding="utf-8")
theme = (ROOT / required[6]).read_text(encoding="utf-8")
history = (ROOT / required[7]).read_text(encoding="utf-8")
history_test = (ROOT / required[8]).read_text(encoding="utf-8")
dao = (ROOT / required[9]).read_text(encoding="utf-8")
repository = (ROOT / required[10]).read_text(encoding="utf-8")
app_build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
database = (ROOT / "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDatabase.kt").read_text(encoding="utf-8")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

checks = {
    "combined phase version": version == "0.8.0-phase7-8" and 'versionName = "0.8.0-phase7-8"' in app_build,
    "accessibility route": "AccessibilitySettings" in view_model and "AccessibilitySettingsScreen" in app_ui,
    "text size preference": "TextSizePreference" in preferences and "setTextSizePreference" in preferences,
    "high contrast preference": "setHighContrast" in preferences and "HighContrastLightColors" in theme,
    "theme receives settings": "textSizePreference = state.textSizePreference" in main_activity and "highContrast = state.highContrast" in main_activity,
    "guided help": "Como preencher" in accessibility_ui and "Muito grande" in accessibility_ui,
    "adaptive action heights": ".heightIn(min =" in app_ui and ".heightIn(min =" in pdf_ui,
    "history search": "ReportHistory.filter" in app_ui and "Pesquisar relatórios" in app_ui,
    "accent-insensitive filter": "Normalizer" in history and "CombiningMarks" in history,
    "next month copy": "copyForNextMonth" in history and "duplicateCompletedForNextMonth" in repository,
    "reopen transaction": "reopenAsOnlyDraft" in dao and "reopenCompletedAsDraft" in repository,
    "safe completed deletion": "deleteCompletedReport" in dao and "deleteCompletedReport" in repository,
    "maintenance confirmations": "MaintenanceConfirmation" in pdf_ui and "O rascunho atual será substituído" in pdf_ui,
    "history tests": "copy for next month" in history_test and "filter matches" in history_test,
    "no Room migration": re.search(r"version\s*=\s*1", database) is not None,
    "offline manifest": "android.permission.INTERNET" not in manifest,
    "no broad storage permission": "READ_EXTERNAL_STORAGE" not in manifest and "WRITE_EXTERNAL_STORAGE" not in manifest,
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

for file in ROOT.rglob("*.json"):
    try:
        json.loads(file.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        errors.append(f"invalid json {file.relative_to(ROOT)}: {error}")

if errors:
    print("Phase 7-8 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Phase 7-8 validation passed")
print("- persistent large/extra-large text and high contrast: present")
print("- in-app guided help and adaptive button heights: present")
print("- searchable history and safe maintenance actions: present")
print("- next-month copy, reopen and completed-only deletion: present")
print("- Room schema remains version 1")
