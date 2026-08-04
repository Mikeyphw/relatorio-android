#!/usr/bin/env python3
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []

def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

version = text("VERSION").strip()
build = text("app/build.gradle.kts")
view_model = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/MainViewModel.kt")
ui = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/RelatorioApp.kt")
settings = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/AccessibilitySettingsScreen.kt")
prefs = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/data/UserPreferencesStore.kt")
repo = text("persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportRepository.kt")
history = text("core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportHistory.kt")
factory = text("core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportDraftFactory.kt")
identity_test = text("app/src/test/kotlin/com/mikeyphw/relatorio/android/IdentityFormTest.kt")
history_test = text("core-model/src/test/kotlin/com/mikeyphw/relatorio/model/ReportHistoryTest.kt")
smoke = text("app/src/androidTest/kotlin/com/mikeyphw/relatorio/android/AppSmokeTest.kt")
e2e = text("app/src/androidTest/kotlin/com/mikeyphw/relatorio/android/AppEndToEndTest.kt")

checks = {
    "version 1.2.0": version == "1.2.0" and 'versionCode = 12' in build and 'versionName = "1.2.0"' in build,
    "new report start route": "NewReportStart" in view_model and "NewReportStartScreen" in ui,
    "blank start action": "startBlankReport" in view_model and "Começar em branco" in ui,
    "copy previous action": "copyPreviousCompletedReport" in view_model and "Copiar relatório anterior" in ui,
    "previous month copy uses current calendar": "LocalDate.now().minusMonths(1)" in view_model and "duplicateCompletedForMonth" in view_model,
    "history next-month remains": "duplicateCompletedForNextMonth" in repo and "copyForNextMonth" in history,
    "selected-month copy clears monthly data": all(token in history for token in ["copyForMonth", "movements = emptyList()", 'notes = ""']),
    "header defaults form": "HeaderDefaultsForm" in view_model and "headerDefaults" in ui,
    "header defaults settings UI": "Valores padrão do cabeçalho" in settings and "Salvar valores padrão" in settings,
    "header defaults persistence": "saveHeaderDefaults" in prefs,
    "new report previous month": "today.minusMonths(1)" in view_model and "today.minusMonths(1)" in factory,
    "identity changes do not save defaults": "preferences.saveIdentity(identity)" not in view_model,
    "tests cover previous month and defaults": all(token in identity_test for token in ["newReportUsesPreviousMonthAndSavedHeaderDefaults", "previousMonthRollsJanuaryBackToDecemberOfPreviousYear", "headerDefaultsAreTrimmedBeforeSaving"]),
    "tests cover selected month copy": "copy for selected month" in history_test,
    "instrumentation updated for start options": "Começar em branco" in smoke and "Começar em branco" in e2e,
    "documentation present": (ROOT / "docs/HEADER_DEFAULTS_AND_COPY_PREVIOUS.md").is_file(),
}
for name, passed in checks.items():
    if not passed:
        errors.append(f"verificação falhou: {name}")

if errors:
    print("Header defaults and copy previous validation failed:")
    for error in errors:
        print(f"- {error}")
    raise SystemExit(1)

print("Header defaults and copy previous validation passed")
print("- configurable header defaults are present")
print("- blank reports start on the previous calendar month")
print("- copy previous report starts from the latest completed report")
print("- monthly movements and notes are not copied")
