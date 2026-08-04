#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []

required = [
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/MainViewModel.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/pdf/ReportPdfExporter.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/pdf/ReportPdfDocumentWriter.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/PdfExportScreen.kt",
    "app/src/main/res/xml/file_paths.xml",
    "core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportPdfContent.kt",
    "core-model/src/test/kotlin/com/mikeyphw/relatorio/model/ReportPdfContentFactoryTest.kt",
    "app/src/androidTest/kotlin/com/mikeyphw/relatorio/android/ReportPdfExporterTest.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDao.kt",
    "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportRepository.kt",
    "docs/PHASE_6_PDF_EXPORT.md",
]
for relative in required:
    if not (ROOT / relative).is_file():
        errors.append(f"missing: {relative}")

if errors:
    print("Phase 6 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

view_model = (ROOT / required[0]).read_text(encoding="utf-8")
exporter = (ROOT / required[1]).read_text(encoding="utf-8")
writer = (ROOT / required[2]).read_text(encoding="utf-8")
export_ui = (ROOT / required[3]).read_text(encoding="utf-8")
paths_xml = (ROOT / required[4]).read_text(encoding="utf-8")
pdf_content = (ROOT / required[5]).read_text(encoding="utf-8")
pdf_test = (ROOT / required[6]).read_text(encoding="utf-8")
android_pdf_test = (ROOT / required[7]).read_text(encoding="utf-8")
dao = (ROOT / required[8]).read_text(encoding="utf-8")
repository = (ROOT / required[9]).read_text(encoding="utf-8")
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
database = (ROOT / "persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDatabase.kt").read_text(encoding="utf-8")
app_ui = (ROOT / "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/RelatorioApp.kt").read_text(encoding="utf-8")
app_build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()

checks = {
    "phase version": version in {"0.6.0-phase6", "0.8.0-phase7-8"} and f'versionName = "{version}"' in app_build,
    "pdf route": "CompletedReportDetail" in view_model and "CompletedReportDetailScreen" in app_ui,
    "completed report loading": "getReport(reportId" in dao and "dao.getReport(reportId)?.toModel()" in repository,
    "completion opens pdf": "route.value = AppRoute.CompletedReportDetail" in view_model,
    "pure content factory": "object ReportPdfContentFactory" in pdf_content,
    "standard split": "Escola Bíblica Dominical" in pdf_content and "ReportPdfSection(\"Cultos\"" in pdf_content,
    "safe file name": "sanitizeFilePart" in pdf_content and ".pdf" in pdf_content,
    "android pdf document": "PdfDocument" in writer and "PageInfo.Builder" in writer,
    "page continuation": "drawContinuationPages" in writer,
    "storage access framework": "ActivityResultContracts.CreateDocument" in export_ui,
    "secure content uri": "FileProvider.getUriForFile" in exporter,
    "temporary grant": "FLAG_GRANT_READ_URI_PERMISSION" in export_ui,
    "file provider manifest": "androidx.core.content.FileProvider" in manifest and "@xml/file_paths" in manifest,
    "narrow cache path": '<cache-path' in paths_xml and 'path="pdf_exports/"' in paths_xml,
    "no broad storage permission": "READ_EXTERNAL_STORAGE" not in manifest and "WRITE_EXTERNAL_STORAGE" not in manifest,
    "offline manifest": "android.permission.INTERNET" not in manifest,
    "pdf content tests": "ReportPdfContentFactoryTest" in pdf_test and "assertEquals(74" in pdf_test,
    "instrumented pdf signature": 'assertEquals("%PDF"' in android_pdf_test,
    "no Room migration": re.search(r"version\s*=\s*1", database) is not None,
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
    print("Phase 6 validation failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Phase 6 validation passed")
print("- pure PDF content for both report templates: present")
print("- A4 PdfDocument renderer with continuation pages: present")
print("- completed-report detail, view, save and share actions: present")
print("- narrow FileProvider and Storage Access Framework: present")
print("- Room schema remains version 1")
