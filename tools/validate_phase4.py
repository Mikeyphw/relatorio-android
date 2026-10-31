#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []

required = [
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/MainViewModel.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/RelatorioApp.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/MovementScreens.kt",
    "core-model/src/main/kotlin/com/mikeyphw/relatorio/model/MemberMovementEditor.kt",
    "core-model/src/test/kotlin/com/mikeyphw/relatorio/model/MemberMovementEditorTest.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDao.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportRepository.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportMappers.kt",
    "docs/PHASE_4_MOVEMENTS_AND_NOTES.md",
]
for relative in required:
    if not (ROOT / relative).is_file():
        errors.append(f"missing: {relative}")

if errors:
    print("Phase 4 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

view_model = (ROOT / required[0]).read_text(encoding="utf-8")
ui = (ROOT / required[1]).read_text(encoding="utf-8")
movement_ui = (ROOT / required[2]).read_text(encoding="utf-8")
editor = (ROOT / required[3]).read_text(encoding="utf-8")
dao = (ROOT / required[5]).read_text(encoding="utf-8")
repository = (ROOT / required[6]).read_text(encoding="utf-8")
mappers = (ROOT / required[7]).read_text(encoding="utf-8")
database = (ROOT / "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDatabase.kt").read_text(encoding="utf-8")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
app_build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()

checks = {
    "phase version": ((re.fullmatch(r"0\.[4-9]\.0-phase[4-9]", version) is not None or version == "0.8.0-phase7-8") or version == "1.0.0") and f'versionName = "{version}"' in app_build,
    "movement routes": all(token in view_model for token in ["Movements", "MovementEditor", "Notes"]),
    "movement actions": all(
        token in view_model
        for token in [
            "openMovements",
            "addMovement",
            "editMovement",
            "saveMovement",
            "deleteMovement",
            "openNotes",
            "updateNotes",
        ]
    ),
    "movement screens wired": all(
        token in ui for token in ["MovementsScreen", "MovementEditorScreen", "NotesScreen"]
    ),
    "plain language reason selector": "MovementReasonCatalog.all" in movement_ui and "Motivo *" in movement_ui,
    "delete confirmation": "Excluir movimentação?" in movement_ui,
    "large add action": "Adicionar pessoa" in movement_ui and "MovementActionHeight" in movement_ui,
    "Brazilian date entry": all(
        token in editor
        for token in ["normalizeTyping", "dd/MM/uuuu", "ISO_LOCAL_DATE", "ResolverStyle.STRICT"]
    ),
    "date validation messages": "dia/mês/ano" in editor,
    "template-specific movement fields": "includesBirthDate" in movement_ui and "movementIncludesBirthDate" in ui,
    "movement persistence": all(
        token in dao
        for token in ["insertMovement", "saveDraftMovement", "deleteDraftMovement", "deleteMovement"]
    ),
    "notes persistence": "updateNotes" in dao and "updateDraftNotes" in repository,
    "movement mapper": "MemberMovement.toEntity" in mappers,
    "notes debounce": "NotesSaveDelayMillis" in view_model and "delay(NotesSaveDelayMillis)" in view_model,
    "no Room migration": re.search(r"version\s*=\s*1", database) is not None,
    "offline manifest": "android.permission.INTERNET" not in manifest,
    "no invalid weight import": "import androidx.compose.foundation.layout.weight" not in movement_ui,
    "movement tests": "MemberMovementEditorTest" in (ROOT / required[4]).read_text(encoding="utf-8"),
}
for name, passed in checks.items():
    if not passed:
        errors.append(f"failed check: {name}")

for file in ROOT.rglob("*.kt"):
    text = file.read_text(encoding="utf-8")
    if "\t" in text:
        errors.append(f"tab character: {file.relative_to(ROOT)}")

if errors:
    print("Phase 4 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Phase 4 validation passed")
print("- movement list, editor and deletion confirmation: present")
print("- Brazilian date validation and ISO storage: present")
print("- model-specific occurrence and birth-date fields: present")
print("- Room movement CRUD and notes update: present")
print("- autosaved notes with delayed writes: present")
print("- Room schema remains version 1")
