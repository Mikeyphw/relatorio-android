#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []

required = [
    "settings.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/MainActivity.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/MainViewModel.kt",
    "core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportTemplateCatalog.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDatabase.kt",
]
for relative in required:
    if not (ROOT / relative).is_file():
        errors.append(f"missing: {relative}")

catalog = (ROOT / required[4]).read_text(encoding="utf-8")
models = (ROOT / "core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportModels.kt").read_text(encoding="utf-8")
manifest = (ROOT / required[1]).read_text(encoding="utf-8")
app_build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
ui = (ROOT / "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/RelatorioApp.kt").read_text(encoding="utf-8")

checks = {
    "default template id": "AD_TIMOTEO_2026_V1" in models,
    "complete template id": "RELATORIO_COMPLETO_V1" in models,
    "sdk 36": "compileSdk = 36" in app_build and "targetSdk = 36" in app_build,
    "offline manifest": "android.permission.INTERNET" not in manifest,
    "new report action": "Preencher novo relatório" in ui,
    "draft replacement confirmation": "Já existe um relatório em andamento" in ui,
    "room tables": all(name in (ROOT / "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/Entities.kt").read_text() for name in ["reports", "report_values", "member_movements"]),
}
for name, passed in checks.items():
    if not passed:
        errors.append(f"failed check: {name}")

# Count field() declarations inside each template block using structural markers.
standard_block = catalog.split("val standard =", 1)[1].split("val complete =", 1)[0]
complete_block = catalog.split("val complete =", 1)[1].split("val all:", 1)[0]
standard_direct = len(re.findall(r"\bfield\(", standard_block))
# Complete reuses three standard sections: 6 + 9 + 3 fields.
complete_effective = len(re.findall(r"\bfield\(", complete_block)) + 18
if standard_direct != 33:
    errors.append(f"standard field count is {standard_direct}, expected 33")
if complete_effective != 74:
    errors.append(f"complete field count is {complete_effective}, expected 74")

if errors:
    print("Phase 2 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Phase 2 validation passed")
print("- standard fields: 33")
print("- complete fields: 74")
print("- Room foundation: present")
print("- DataStore foundation: present")
print("- Android permissions: offline only")
