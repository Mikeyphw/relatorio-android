#!/usr/bin/env python3
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
validators = [
    "validate_phase2.py",
    "validate_phase3.py",
    "validate_phase4.py",
    "validate_phase5.py",
    "validate_phase6.py",
    "validate_phase7_8.py",
    "validate_phase9_10.py",
]
errors: list[str] = []

for validator in validators:
    result = subprocess.run(
        [sys.executable, str(ROOT / "tools" / validator)],
        cwd=ROOT,
        text=True,
        capture_output=True,
        check=False,
    )
    if result.returncode != 0:
        errors.append(f"{validator}:\n{result.stdout}{result.stderr}")

required = [
    "docs/PHASE_1_TO_10_GAP_CLOSURE.md",
    "docs/REAL_DEVICE_ACCEPTANCE.md",
    "app/src/androidTest/kotlin/com/mikeyphw/relatorio/android/AppEndToEndTest.kt",
    "app/src/androidTest/kotlin/com/mikeyphw/relatorio/android/TestAppDataResetRule.kt",
]
for relative in required:
    if not (ROOT / relative).is_file():
        errors.append(f"arquivo ausente: {relative}")

version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
app_ui = (ROOT / "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/RelatorioApp.kt").read_text(encoding="utf-8")
review_ui = (ROOT / "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/ReviewScreen.kt").read_text(encoding="utf-8")
backup_model = (ROOT / "core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportBackup.kt").read_text(encoding="utf-8")
backup_ui = (ROOT / "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/BackupRestoreScreen.kt").read_text(encoding="utf-8")
smoke = (ROOT / "app/src/androidTest/kotlin/com/mikeyphw/relatorio/android/AppSmokeTest.kt").read_text(encoding="utf-8")
e2e = (ROOT / "app/src/androidTest/kotlin/com/mikeyphw/relatorio/android/AppEndToEndTest.kt").read_text(encoding="utf-8")

checks = {
    "version 1.0.1": version == "1.0.1" and 'versionCode = 11' in build and 'versionName = "1.0.1"' in build,
    "preference-only restore": all(token in backup_model for token in ["willRestorePreferences", "restoredPreferences"]),
    "preference restore UI": "Restaurar configurações" in backup_ui and "configurações também" in backup_ui,
    "contextual help": "contextualHelpFor" in app_ui and 'Text("Ajuda")' in app_ui,
    "three primary home actions": app_ui.count("LargeActionButton(", app_ui.index("private fun HomeScreen"), app_ui.index("private fun TemplatePickerScreen")) == 3,
    "secondary options menu": 'Text("Mais opções")' in app_ui,
    "auto mirrored icons": all(token not in app_ui + review_ui for token in ["Icons.Default.Article", "Icons.Default.NavigateNext", "Icons.Default.FactCheck"]),
    "Compose test v2": "junit4.v2.createAndroidComposeRule" in smoke and "junit4.v2.createAndroidComposeRule" in e2e,
    "end-to-end flow": all(token in e2e for token in ["numeric_next", "movement_name", "review_complete", "PDF do relatório", "Backup e restauração"]),
}
for name, passed in checks.items():
    if not passed:
        errors.append(f"verificação falhou: {name}")

for file in ROOT.rglob("*.kt"):
    text = file.read_text(encoding="utf-8")
    if "import androidx.compose.foundation.layout.weight" in text:
        errors.append(f"importação inválida: {file.relative_to(ROOT)}")
    if "androidx.compose.ui.test.junit4.createAndroidComposeRule" in text:
        errors.append(f"Compose Test v1 restante: {file.relative_to(ROOT)}")

if errors:
    print("Final Phase 1-10 validation failed:")
    for error in errors:
        print(f"- {error}")
    raise SystemExit(1)

print("Final Phase 1-10 validation passed")
print("- all historical validators pass on the final 1.0.1 tree")
print("- preference-only restoration is enabled")
print("- contextual help and three primary home actions are present")
print("- deprecated Compose APIs are removed")
print("- end-to-end instrumentation regression is present")
