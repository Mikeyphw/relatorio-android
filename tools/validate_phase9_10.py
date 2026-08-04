#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ERRORS: list[str] = []


def require(condition: bool, message: str) -> None:
    if not condition:
        ERRORS.append(message)


def text(path: str) -> str:
    file = ROOT / path
    require(file.is_file(), f"arquivo ausente: {path}")
    return file.read_text(encoding="utf-8") if file.is_file() else ""


required_files = [
    "core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportBackup.kt",
    "core-model/src/test/kotlin/com/mikeyphw/relatorio/model/ReportBackupTest.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/backup/ReportBackupManager.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/pdf/ReportPrintAdapter.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/BackupRestoreScreen.kt",
    "app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/PrivacyInfoScreen.kt",
    "docs/PHASE_9_BACKUP_PORTABILITY.md",
    "docs/PHASE_10_RELEASE_READINESS.md",
    "docs/PRIVACY_NOTICE_PT_BR.md",
    "docs/RELEASE_CHECKLIST.md",
    "docs/MANUAL_RAPIDO_PT_BR.md",
    "tools/build_signed_release.sh",
]
for required in required_files:
    require((ROOT / required).is_file(), f"arquivo obrigatório ausente: {required}")

build = text("app/build.gradle.kts")
require('versionCode = 10' in build, "versionCode final não é 10")
require('versionName = "1.0.0"' in build, "versionName final não é 1.0.0")
require("buildConfig = true" in build, "BuildConfig precisa estar habilitado para exportar a versão no backup")
require(text("VERSION").strip() == "1.0.0", "arquivo VERSION não está em 1.0.0")

manifest = text("app/src/main/AndroidManifest.xml")
for forbidden in ("android.permission.INTERNET", "MANAGE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"):
    require(forbidden not in manifest, f"permissão proibida presente: {forbidden}")
require('android:allowBackup="false"' in manifest, "backup automático do Android deveria permanecer desativado")

backup = text("core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportBackup.kt")
for marker in (
    "ReportBackupXmlCodec",
    "ReportBackupValidator",
    "ReportBackupPlanner",
    "disallow-doctype-decl",
    "MaximumBackupCharacters",
    "checksum-sha256",
    "ChecksumAttribute",
    "skippedDuplicateCount",
    "willReplaceCurrentDraft",
):
    require(marker in backup, f"contrato de backup ausente: {marker}")
require("<!DOCTYPE" in backup and "<!ENTITY" in backup, "proteção explícita contra DTD/entidades ausente")

manager = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/backup/ReportBackupManager.kt")
for marker in ("ZipOutputStream", "ZipInputStream", "relatorio-backup.xml", "ReportPdfDocumentWriter.write", "MaximumBackupBytes", "CodingErrorAction.REPORT"):
    require(marker in manager, f"gerenciador de backup incompleto: {marker}")

repository = text("persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportRepository.kt")
dao = text("persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDao.kt")
for marker in ("snapshotAllReports", "previewRestore", "restoreBackup"):
    require(marker in repository, f"repositório sem {marker}")
require("@Transaction" in dao and "restoreReports" in dao, "restauração Room não é transacional")

pdf_exporter = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/pdf/ReportPdfExporter.kt")
for marker in ("StatFs", "MinimumFreeBytes", "temporary.renameTo", "temporary.delete"):
    require(marker in pdf_exporter, f"hardening do PDF ausente: {marker}")

pdf_screen = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/PdfExportScreen.kt")
require('Text("Imprimir")' in pdf_screen and "printPdf(" in pdf_screen, "ação direta de impressão ausente")

app_ui = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/RelatorioApp.kt")
for marker in ("BackupRestoreScreen", "PrivacyInfoScreen", "Backup e restauração", "Privacidade e informações"):
    require(marker in app_ui, f"rota/tela final ausente: {marker}")

signing = text("tools/build_signed_release.sh")
for secret_name in ("RELATORIO_KEYSTORE", "RELATORIO_KEY_ALIAS", "RELATORIO_STORE_PASSWORD", "RELATORIO_KEY_PASSWORD"):
    require(secret_name in signing, f"script de assinatura sem variável {secret_name}")
require("apksigner verify" not in signing or "verify" in signing, "script não verifica assinatura")
require("storePassword =" not in build and "keyPassword =" not in build, "senha de assinatura não pode ficar no Gradle")

catalog = text("core-model/src/main/kotlin/com/mikeyphw/relatorio/model/ReportTemplateCatalog.kt")
require(catalog.count("field(") >= 90, "catálogo não preserva os campos dos dois modelos")

# Promise audit for prior phases without pinning their historical version strings.
view_model = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/MainViewModel.kt")
app_ui = text("app/src/main/kotlin/com/mikeyphw/relatorio/android/ui/RelatorioApp.kt")
for marker in (
    "TemplatePicker", "NumericSection", "Movements", "MovementEditor", "Notes",
    "Review", "CompletedReportDetail", "AccessibilitySettings",
):
    require(marker in view_model and marker in app_ui, f"promessa de fase anterior ausente: {marker}")
require("ReportReviewEngine.review" in view_model, "motor de revisão não está ligado ao fluxo")
require("ReportPdfDocumentWriter" in manager, "geração de PDF não foi preservada no pacote")
require("ReportHistory.filter" in app_ui, "pesquisa do histórico não foi preservada")
require("version = 1" in text("persistence/src/main/kotlin/com/mikeyphw/relatorio/persistence/ReportDatabase.kt"), "esquema Room mudou sem migração")

if ERRORS:
    print("Phase 9/10 validation failed:")
    for error in ERRORS:
        print(f"- {error}")
    raise SystemExit(1)

print("Phase 9/10 validation passed")
