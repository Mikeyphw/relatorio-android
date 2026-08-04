package com.mikeyphw.relatorio.model

import java.io.StringReader
import java.text.Normalizer
import java.security.MessageDigest
import java.time.LocalDate
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

const val CurrentBackupFormatVersion = 1
const val MaximumBackupCharacters = 8_000_000

/** Preferences that are safe and useful when moving the app to another device. */
data class BackupPreferencesSnapshot(
    val defaultTemplateId: String = ReportTemplateId.AdTimoteo2026.stableId,
    val regional: String = "",
    val congregation: String = "",
    val leadPastor: String = "",
    val ministry: String = "Ministério AD Timóteo MG",
    val presidentPastor: String = "Carmo Matias Gonçalves",
    val textSizePreferenceId: String = "large",
    val highContrast: Boolean = false,
    val draftProgressReportId: String = "",
    val draftProgressCurrentSection: Int = 0,
    val draftProgressCompletedSections: Int = 0,
)

data class ReportBackupArchive(
    val formatVersion: Int = CurrentBackupFormatVersion,
    val createdAtEpochMillis: Long,
    val appVersion: String,
    val reports: List<MonthlyReport>,
    val preferences: BackupPreferencesSnapshot,
)

data class BackupValidationResult(
    val blockingErrors: List<String>,
    val warnings: List<String>,
) {
    val isValid: Boolean get() = blockingErrors.isEmpty()
}

data class BackupRestorePreview(
    val totalReportsInBackup: Int,
    val completedReportsInBackup: Int,
    val draftReportsInBackup: Int,
    val importableReports: List<MonthlyReport>,
    val skippedDuplicateCount: Int,
    val skippedDraftCount: Int,
    val willReplaceCurrentDraft: Boolean,
    val blockingErrors: List<String>,
    val warnings: List<String>,
) {
    val canRestore: Boolean get() = blockingErrors.isEmpty() && importableReports.isNotEmpty()
}

data class BackupRestoreResult(
    val restoredReportCount: Int,
    val skippedDuplicateCount: Int,
    val restoredDraftId: String?,
)

object ReportBackupValidator {
    fun validate(archive: ReportBackupArchive): BackupValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (archive.formatVersion != CurrentBackupFormatVersion) {
            errors += "Este backup usa uma versão não compatível (${archive.formatVersion})."
        }
        if (archive.createdAtEpochMillis < 0L) {
            errors += "O backup contém uma data interna inválida."
        }
        if (archive.appVersion.length > MaximumAppVersionLength) {
            errors += "A versão do aplicativo registrada no backup é longa demais."
        }
        if (ReportTemplateId.entries.none { it.stableId == archive.preferences.defaultTemplateId }) {
            errors += "O backup indica um formulário padrão desconhecido."
        }
        if (archive.preferences.textSizePreferenceId !in SupportedTextSizePreferences) {
            errors += "O backup indica um tamanho de texto desconhecido."
        }
        if (archive.preferences.draftProgressCurrentSection < 0 ||
            archive.preferences.draftProgressCompletedSections < 0
        ) {
            errors += "O backup contém um progresso de rascunho inválido."
        }
        if (archive.reports.size > MaximumReports) {
            errors += "O backup contém relatórios demais (${archive.reports.size})."
        }
        if (archive.reports.count { it.status == ReportStatus.Draft } > 1) {
            errors += "O backup contém mais de um relatório em andamento."
        }
        if (archive.reports.map { it.id }.toSet().size != archive.reports.size) {
            errors += "O backup contém identificadores de relatório repetidos."
        }
        listOf(
            archive.preferences.regional,
            archive.preferences.congregation,
            archive.preferences.leadPastor,
            archive.preferences.ministry,
            archive.preferences.presidentPastor,
        ).forEach { value ->
            if (value.length > MaximumIdentityTextLength) {
                errors += "O backup contém uma preferência de identificação longa demais."
            }
        }
        val allMovementIds = archive.reports.flatMap { report -> report.movements.map { it.id } }
        if (allMovementIds.toSet().size != allMovementIds.size) {
            errors += "O backup contém identificadores de movimentação repetidos entre relatórios."
        }

        archive.reports.forEachIndexed { index, report ->
            val prefix = "Relatório ${index + 1}"
            val definition = ReportTemplateCatalog.definition(report.templateId)
            val identityErrors = ReportIdentityValidator.validate(report.identity)
            if (identityErrors.isNotEmpty()) {
                errors += identityErrors.map { "$prefix: $it" }
            }
            if (report.id.isBlank() || report.id.length > 120) {
                errors += "$prefix: identificador inválido."
            }
            listOf(
                report.identity.regional,
                report.identity.congregation,
                report.identity.leadPastor,
                report.identity.ministry,
                report.identity.presidentPastor,
            ).forEach { value ->
                if (value.length > MaximumIdentityTextLength) {
                    errors += "$prefix: um dado de identificação é longo demais."
                }
            }
            if (report.createdAtEpochMillis < 0L || report.updatedAtEpochMillis < 0L) {
                errors += "$prefix: datas internas inválidas."
            }
            if (report.notes.length > MaximumNotesLength) {
                errors += "$prefix: anotações excedem o limite permitido."
            }
            if (report.values.size != definition.fieldCount) {
                errors += "$prefix: quantidade de campos incompatível com o formulário."
            }
            val fieldIds = report.values.map { it.fieldId }
            if (fieldIds.toSet().size != fieldIds.size || fieldIds.toSet() != definition.fieldIds) {
                errors += "$prefix: campos numéricos ausentes, repetidos ou desconhecidos."
            }
            report.values.forEach { value ->
                if (value.value !in 0..MaximumReportValue) {
                    errors += "$prefix: o campo ${value.fieldId} contém um número inválido."
                }
                if ((value.customLabel?.length ?: 0) > MaximumCustomLabelLength) {
                    errors += "$prefix: um nome personalizado é longo demais."
                }
            }
            if (report.movements.size > MaximumMovementsPerReport) {
                errors += "$prefix: há movimentações demais."
            }
            if (report.movements.map { it.id }.toSet().size != report.movements.size) {
                errors += "$prefix: existem movimentações repetidas."
            }
            report.movements.forEach { movement ->
                if (movement.id.isBlank() || movement.id.length > 120) errors += "$prefix: identificador de movimentação inválido."
                if (movement.memberName.isBlank()) errors += "$prefix: há uma movimentação sem nome."
                if (movement.memberName.length > 160) errors += "$prefix: há um nome longo demais."
                if (movement.registrationNumber.length > 80) errors += "$prefix: há um número de registro longo demais."
                if (MovementReasonCatalog.byCode(movement.reasonCode) == null) {
                    errors += "$prefix: código de movimentação desconhecido (${movement.reasonCode})."
                }
                if (movement.occurrence.length > 240) errors += "$prefix: ocorrência longa demais."
                if (!movement.eventDateIso.isValidOptionalIsoDate()) errors += "$prefix: data de movimentação inválida."
                if (!movement.birthDateIso.isValidOptionalIsoDate()) errors += "$prefix: data de nascimento inválida."
            }
        }

        val naturalKeys = archive.reports
            .filter { it.status == ReportStatus.Completed }
            .map(::naturalKey)
        if (naturalKeys.toSet().size != naturalKeys.size) {
            warnings += "O backup contém relatórios concluídos equivalentes para o mesmo mês e congregação."
        }

        return BackupValidationResult(errors.distinct(), warnings.distinct())
    }

    private const val MaximumReports = 600
    private const val MaximumMovementsPerReport = 2_000
    private const val MaximumAppVersionLength = 80
    private val SupportedTextSizePreferences = setOf("large", "extra_large")
    private const val MaximumNotesLength = 4_000
    private const val MaximumCustomLabelLength = 80
    private const val MaximumReportValue = 999_999
    private const val MaximumIdentityTextLength = 200

    private fun String.isValidOptionalIsoDate(): Boolean =
        isBlank() || runCatching { LocalDate.parse(this) }.isSuccess
}

object ReportBackupPlanner {
    fun preview(
        archive: ReportBackupArchive,
        existingReports: List<MonthlyReport>,
        replaceCurrentDraft: Boolean,
    ): BackupRestorePreview {
        val validation = ReportBackupValidator.validate(archive)
        if (!validation.isValid) {
            return BackupRestorePreview(
                totalReportsInBackup = archive.reports.size,
                completedReportsInBackup = archive.reports.count { it.status == ReportStatus.Completed },
                draftReportsInBackup = archive.reports.count { it.status == ReportStatus.Draft },
                importableReports = emptyList(),
                skippedDuplicateCount = 0,
                skippedDraftCount = 0,
                willReplaceCurrentDraft = false,
                blockingErrors = validation.blockingErrors,
                warnings = validation.warnings,
            )
        }

        val existingIds = existingReports.map { it.id }.toSet()
        val currentDraft = existingReports.singleOrNull { it.status == ReportStatus.Draft }
        val backupDraft = archive.reports.singleOrNull { it.status == ReportStatus.Draft }
        val willReplaceExistingDraft = replaceCurrentDraft && currentDraft != null && backupDraft != null
        val existingCompletedKeys = existingReports
            .filter { it.status == ReportStatus.Completed }
            .map(::naturalKey)
            .toMutableSet()
        val existingMovementIds = existingReports
            .filterNot { willReplaceExistingDraft && it.status == ReportStatus.Draft }
            .flatMap { report -> report.movements.map { it.id } }
            .toMutableSet()
        val acceptedIds = mutableSetOf<String>()
        val acceptedMovementIds = mutableSetOf<String>()
        val acceptedCompletedKeys = mutableSetOf<String>()
        val importable = mutableListOf<MonthlyReport>()
        var duplicates = 0
        var skippedDrafts = 0

        archive.reports
            .sortedWith(compareBy<MonthlyReport> { it.status != ReportStatus.Draft }.thenBy { it.createdAtEpochMillis })
            .forEach { report ->
                val replacesSameDraftId = willReplaceExistingDraft &&
                    report.status == ReportStatus.Draft &&
                    report.id == currentDraft?.id
                if ((report.id in existingIds && !replacesSameDraftId) || !acceptedIds.add(report.id)) {
                    duplicates += 1
                    return@forEach
                }
                val movementIds = report.movements.map { it.id }
                if (movementIds.any { it in existingMovementIds || it in acceptedMovementIds }) {
                    duplicates += 1
                    return@forEach
                }
                acceptedMovementIds += movementIds
                if (report.status == ReportStatus.Draft) {
                    if (currentDraft != null && !willReplaceExistingDraft) {
                        skippedDrafts += 1
                        return@forEach
                    }
                    importable += report
                    return@forEach
                }

                val key = naturalKey(report)
                if (key in existingCompletedKeys || !acceptedCompletedKeys.add(key)) {
                    duplicates += 1
                } else {
                    importable += report
                }
            }

        val warnings = buildList {
            addAll(validation.warnings)
            if (duplicates > 0) add("$duplicates relatório(s) repetido(s) serão ignorados.")
            if (skippedDrafts > 0) add("O relatório em andamento do backup não será restaurado sem autorização para substituir o rascunho atual.")
            if (archive.reports.isNotEmpty() && importable.isEmpty()) add("Não há relatórios novos para restaurar.")
        }

        return BackupRestorePreview(
            totalReportsInBackup = archive.reports.size,
            completedReportsInBackup = archive.reports.count { it.status == ReportStatus.Completed },
            draftReportsInBackup = archive.reports.count { it.status == ReportStatus.Draft },
            importableReports = importable,
            skippedDuplicateCount = duplicates,
            skippedDraftCount = skippedDrafts,
            willReplaceCurrentDraft = willReplaceExistingDraft && importable.any { it.status == ReportStatus.Draft },
            blockingErrors = emptyList(),
            warnings = warnings,
        )
    }
}

object ReportBackupXmlCodec {
    fun encode(archive: ReportBackupArchive): String {
        val validation = ReportBackupValidator.validate(archive)
        require(validation.isValid) { validation.blockingErrors.joinToString(" ") }
        val unsigned = buildXml(archive, ChecksumPlaceholder)
        val checksum = unsigned.sha256()
        return unsigned.replaceFirst(ChecksumPlaceholder, checksum)
    }

    private fun buildXml(archive: ReportBackupArchive, checksum: String): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<relatorio-backup format-version=\"")
        append(archive.formatVersion)
        append("\" created-at=\"")
        append(archive.createdAtEpochMillis)
        append("\" app-version=\"")
        appendXmlAttribute(archive.appVersion)
        append("\" checksum-sha256=\"")
        append(checksum)
        append("\">\n")
        appendPreferences(archive.preferences)
        append("  <reports>\n")
        archive.reports.forEach { appendReport(it) }
        append("  </reports>\n")
        append("</relatorio-backup>\n")
    }


    fun decode(xml: String): ReportBackupArchive {
        require(xml.length <= MaximumBackupCharacters) { "O arquivo de backup é grande demais." }
        val checksumMatch = ChecksumAttribute.find(xml)
            ?: error("O backup não contém a verificação de integridade.")
        require(ChecksumAttribute.findAll(xml).count() == 1) { "O backup contém verificações de integridade repetidas." }
        val expectedChecksum = checksumMatch.groupValues[1].lowercase()
        val unsignedXml = xml.replaceRange(checksumMatch.groups[1]!!.range, ChecksumPlaceholder)
        require(unsignedXml.sha256() == expectedChecksum) { "O arquivo de backup está corrompido ou foi alterado." }
        require(!xml.contains("<!DOCTYPE", ignoreCase = true)) { "O arquivo contém uma declaração XML não permitida." }
        require(!xml.contains("<!ENTITY", ignoreCase = true)) { "O arquivo contém entidades XML não permitidas." }

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isXIncludeAware = false
            isExpandEntityReferences = false
            runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "") }
            runCatching { setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "") }
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val root = document.documentElement
        require(root.tagName == "relatorio-backup") { "Este arquivo não é um backup do Relatório Mensal AD." }

        val formatVersion = root.requiredIntAttribute("format-version")
        val createdAt = root.requiredLongAttribute("created-at")
        val appVersion = root.getAttribute("app-version")
        val preferences = root.directChild("preferences").toPreferences()
        val reportsElement = root.directChild("reports")
        val reports = reportsElement.directChildren("report").map { it.toReport() }
        val archive = ReportBackupArchive(
            formatVersion = formatVersion,
            createdAtEpochMillis = createdAt,
            appVersion = appVersion,
            reports = reports,
            preferences = preferences,
        )
        val validation = ReportBackupValidator.validate(archive)
        require(validation.isValid) { validation.blockingErrors.joinToString(" ") }
        return archive
    }

    private fun StringBuilder.appendPreferences(snapshot: BackupPreferencesSnapshot) {
        append("  <preferences")
        appendAttribute("default-template", snapshot.defaultTemplateId)
        appendAttribute("regional", snapshot.regional)
        appendAttribute("congregation", snapshot.congregation)
        appendAttribute("lead-pastor", snapshot.leadPastor)
        appendAttribute("ministry", snapshot.ministry)
        appendAttribute("president-pastor", snapshot.presidentPastor)
        appendAttribute("text-size", snapshot.textSizePreferenceId)
        appendAttribute("high-contrast", snapshot.highContrast.toString())
        appendAttribute("draft-progress-report-id", snapshot.draftProgressReportId)
        appendAttribute("draft-progress-current-section", snapshot.draftProgressCurrentSection.toString())
        appendAttribute("draft-progress-completed-sections", snapshot.draftProgressCompletedSections.toString())
        append(" />\n")
    }

    private fun StringBuilder.appendReport(report: MonthlyReport) {
        append("    <report")
        appendAttribute("id", report.id)
        appendAttribute("template-id", report.templateId.stableId)
        appendAttribute("status", report.status.storageValue)
        appendAttribute("month", report.identity.month.toString())
        appendAttribute("year", report.identity.year.toString())
        appendAttribute("regional", report.identity.regional)
        appendAttribute("congregation", report.identity.congregation)
        appendAttribute("lead-pastor", report.identity.leadPastor)
        appendAttribute("ministry", report.identity.ministry)
        appendAttribute("president-pastor", report.identity.presidentPastor)
        appendAttribute("created-at", report.createdAtEpochMillis.toString())
        appendAttribute("updated-at", report.updatedAtEpochMillis.toString())
        append(">\n")
        append("      <values>\n")
        report.values.sortedBy { it.fieldId }.forEach { value ->
            append("        <value")
            appendAttribute("field-id", value.fieldId)
            appendAttribute("number", value.value.toString())
            value.customLabel?.let { appendAttribute("custom-label", it) }
            append(" />\n")
        }
        append("      </values>\n")
        append("      <movements>\n")
        report.movements.forEach { movement ->
            append("        <movement")
            appendAttribute("id", movement.id)
            appendAttribute("registration-number", movement.registrationNumber)
            appendAttribute("member-name", movement.memberName)
            appendAttribute("event-date", movement.eventDateIso)
            appendAttribute("reason-code", movement.reasonCode)
            appendAttribute("occurrence", movement.occurrence)
            appendAttribute("birth-date", movement.birthDateIso)
            append(" />\n")
        }
        append("      </movements>\n")
        append("      <notes>")
        appendXmlText(report.notes)
        append("</notes>\n")
        append("    </report>\n")
    }

    private fun Element.toPreferences(): BackupPreferencesSnapshot {
        val highContrastText = requiredAttribute("high-contrast")
        return BackupPreferencesSnapshot(
            defaultTemplateId = requiredAttribute("default-template"),
            regional = getAttribute("regional"),
            congregation = getAttribute("congregation"),
            leadPastor = getAttribute("lead-pastor"),
            ministry = getAttribute("ministry"),
            presidentPastor = getAttribute("president-pastor"),
            textSizePreferenceId = requiredAttribute("text-size"),
            highContrast = highContrastText.toBooleanStrictOrNull()
                ?: error("Valor booleano inválido no atributo high-contrast"),
            draftProgressReportId = getAttribute("draft-progress-report-id"),
            draftProgressCurrentSection = optionalIntAttribute("draft-progress-current-section"),
            draftProgressCompletedSections = optionalIntAttribute("draft-progress-completed-sections"),
        )
    }

    private fun Element.toReport(): MonthlyReport {
        val templateIdText = requiredAttribute("template-id")
        val templateId = ReportTemplateId.entries.firstOrNull { it.stableId == templateIdText }
            ?: error("Formulário desconhecido no backup: $templateIdText")
        val statusText = requiredAttribute("status")
        val status = ReportStatus.entries.firstOrNull { it.storageValue == statusText }
            ?: error("Estado de relatório desconhecido no backup: $statusText")
        val values = directChild("values").directChildren("value").map { value ->
            ReportValue(
                fieldId = value.requiredAttribute("field-id"),
                value = value.requiredIntAttribute("number"),
                customLabel = value.getAttribute("custom-label").ifBlank { null },
            )
        }
        val movements = directChild("movements").directChildren("movement").map { movement ->
            MemberMovement(
                id = movement.requiredAttribute("id"),
                registrationNumber = movement.getAttribute("registration-number"),
                memberName = movement.requiredAttribute("member-name"),
                eventDateIso = movement.getAttribute("event-date"),
                reasonCode = movement.requiredAttribute("reason-code"),
                occurrence = movement.getAttribute("occurrence"),
                birthDateIso = movement.getAttribute("birth-date"),
            )
        }
        return MonthlyReport(
            id = requiredAttribute("id"),
            templateId = templateId,
            status = status,
            identity = ReportIdentity(
                month = requiredIntAttribute("month"),
                year = requiredIntAttribute("year"),
                regional = getAttribute("regional"),
                congregation = requiredAttribute("congregation"),
                leadPastor = getAttribute("lead-pastor"),
                ministry = getAttribute("ministry"),
                presidentPastor = getAttribute("president-pastor"),
            ),
            values = values,
            movements = movements,
            notes = directChild("notes").textContent.orEmpty(),
            createdAtEpochMillis = requiredLongAttribute("created-at"),
            updatedAtEpochMillis = requiredLongAttribute("updated-at"),
        )
    }

    private fun Element.directChild(tag: String): Element = directChildren(tag).singleOrNull()
        ?: error("Elemento obrigatório ausente ou repetido: $tag")

    private fun Element.directChildren(tag: String): List<Element> = buildList {
        val children = childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            if (node is Element && node.tagName == tag) add(node)
        }
    }

    private fun Element.requiredAttribute(name: String): String = getAttribute(name).also {
        require(it.isNotBlank()) { "Atributo obrigatório ausente: $name" }
    }

    private fun Element.requiredIntAttribute(name: String): Int = requiredAttribute(name).toIntOrNull()
        ?: error("Número inválido no atributo $name")

    private fun Element.requiredLongAttribute(name: String): Long = requiredAttribute(name).toLongOrNull()
        ?: error("Número inválido no atributo $name")

    private fun Element.optionalIntAttribute(name: String): Int {
        val raw = getAttribute(name)
        if (raw.isBlank()) return 0
        return raw.toIntOrNull() ?: error("Número inválido no atributo $name")
    }

    private fun StringBuilder.appendAttribute(name: String, value: String) {
        append(' ')
        append(name)
        append("=\"")
        appendXmlAttribute(value)
        append('"')
    }

    private fun StringBuilder.appendXmlAttribute(value: String) {
        appendXmlEscaped(value, escapeQuotes = true)
    }

    private fun StringBuilder.appendXmlText(value: String) {
        appendXmlEscaped(value, escapeQuotes = false)
    }

    private fun StringBuilder.appendXmlEscaped(value: String, escapeQuotes: Boolean) {
        var index = 0
        while (index < value.length) {
            val codePoint = Character.codePointAt(value, index)
            when (codePoint) {
                '&'.code -> append("&amp;")
                '<'.code -> append("&lt;")
                '>'.code -> append("&gt;")
                '"'.code -> if (escapeQuotes) append("&quot;") else append('"')
                '\''.code -> if (escapeQuotes) append("&apos;") else append('\'')
                else -> if (codePoint.isAllowedXmlCodePoint()) appendCodePoint(codePoint) else append(' ')
            }
            index += Character.charCount(codePoint)
        }
    }

    private fun Int.isAllowedXmlCodePoint(): Boolean =
        this == 0x9 || this == 0xA || this == 0xD ||
            this in 0x20..0xD7FF || this in 0xE000..0xFFFD || this in 0x10000..0x10FFFF

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private const val ChecksumPlaceholder =
        "0000000000000000000000000000000000000000000000000000000000000000"
    private val ChecksumAttribute = Regex("""checksum-sha256="([0-9a-fA-F]{64})"""")
}

private fun naturalKey(report: MonthlyReport): String = listOf(
    report.templateId.stableId,
    report.identity.year.toString(),
    report.identity.month.toString(),
    normalizeKey(report.identity.congregation),
).joinToString("|")

private fun normalizeKey(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .lowercase()
    .replace("\\s+".toRegex(), " ")
