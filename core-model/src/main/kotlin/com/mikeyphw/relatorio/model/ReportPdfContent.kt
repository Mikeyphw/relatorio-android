package com.mikeyphw.relatorio.model

import java.text.Normalizer

/** Platform-neutral representation consumed by the Android PDF renderer. */
data class ReportPdfContent(
    val templateId: ReportTemplateId,
    val titlePtBr: String,
    val ministry: String,
    val presidentPastor: String,
    val periodPtBr: String,
    val regional: String,
    val congregation: String,
    val leadPastor: String,
    val sections: List<ReportPdfSection>,
    val movements: List<ReportPdfMovement>,
    val movementIncludesBirthDate: Boolean,
    val notes: String,
    val suggestedFileName: String,
)

data class ReportPdfSection(
    val titlePtBr: String,
    val rows: List<ReportPdfFieldRow>,
)

data class ReportPdfFieldRow(
    val order: Int,
    val labelPtBr: String,
    val value: Int,
)

data class ReportPdfMovement(
    val registrationNumber: String,
    val memberName: String,
    val eventDatePtBr: String,
    val reasonCode: String,
    val reasonLabelPtBr: String,
    val occurrence: String,
    val birthDatePtBr: String,
)

object ReportPdfContentFactory {
    private val monthNames = listOf(
        "Janeiro",
        "Fevereiro",
        "Março",
        "Abril",
        "Maio",
        "Junho",
        "Julho",
        "Agosto",
        "Setembro",
        "Outubro",
        "Novembro",
        "Dezembro",
    )

    fun create(report: MonthlyReport): ReportPdfContent {
        val definition = ReportTemplateCatalog.definition(report.templateId)
        val values = report.values.associateBy(ReportValue::fieldId)
        val sections = definition.sections.flatMap { section ->
            val rows = section.fields.mapIndexed { index, field ->
                val fieldId = "${section.id}.${field.id}"
                val stored = values[fieldId]
                ReportPdfFieldRow(
                    order = index + 1,
                    labelPtBr = stored?.customLabel?.trim().orEmpty().ifBlank { field.labelPtBr },
                    value = stored?.value?.coerceAtLeast(0) ?: 0,
                )
            }
            splitCombinedStandardSection(report.templateId, section.titlePtBr, rows)
        }
        val movements = report.movements.map { movement ->
            val reason = MovementReasonCatalog.byCode(movement.reasonCode)
            ReportPdfMovement(
                registrationNumber = movement.registrationNumber.trim(),
                memberName = movement.memberName.trim(),
                eventDatePtBr = isoDateToPtBr(movement.eventDateIso),
                reasonCode = movement.reasonCode.trim().uppercase(),
                reasonLabelPtBr = reason?.labelPtBr ?: movement.reasonCode.trim(),
                occurrence = movement.occurrence.trim().ifBlank { reason?.labelPtBr.orEmpty() },
                birthDatePtBr = isoDateToPtBr(movement.birthDateIso),
            )
        }
        val month = monthNames.getOrElse(report.identity.month - 1) { "Mês ${report.identity.month}" }
        return ReportPdfContent(
            templateId = report.templateId,
            titlePtBr = "RELATÓRIO MENSAL DOS TRABALHOS REALIZADOS",
            ministry = report.identity.ministry.trim(),
            presidentPastor = report.identity.presidentPastor.trim(),
            periodPtBr = "$month de ${report.identity.year}",
            regional = report.identity.regional.trim(),
            congregation = report.identity.congregation.trim(),
            leadPastor = report.identity.leadPastor.trim(),
            sections = sections,
            movements = movements,
            movementIncludesBirthDate = definition.movementIncludesBirthDate,
            notes = report.notes.trim(),
            suggestedFileName = suggestedFileName(report),
        )
    }

    fun suggestedFileName(report: MonthlyReport): String {
        val model = when (report.templateId) {
            ReportTemplateId.AdTimoteo2026 -> "AD_Timoteo"
            ReportTemplateId.Complete -> "Completo"
        }
        val congregation = sanitizeFilePart(report.identity.congregation).ifBlank { "Congregacao" }
        val month = report.identity.month.coerceIn(1, 12).toString().padStart(2, '0')
        return "Relatorio_${model}_${congregation}_${report.identity.year}-$month.pdf".take(120)
    }

    private fun splitCombinedStandardSection(
        templateId: ReportTemplateId,
        titlePtBr: String,
        rows: List<ReportPdfFieldRow>,
    ): List<ReportPdfSection> {
        if (
            templateId != ReportTemplateId.AdTimoteo2026 ||
            titlePtBr != "Escola Bíblica Dominical e cultos" ||
            rows.size != 10
        ) {
            return listOf(ReportPdfSection(titlePtBr, rows))
        }
        val sundaySchool = rows.take(5).mapIndexed { index, row ->
            row.copy(order = index + 1, labelPtBr = row.labelPtBr.removePrefix("EBD — "))
        }
        val services = rows.drop(5).mapIndexed { index, row ->
            row.copy(order = index + 1, labelPtBr = row.labelPtBr.removePrefix("Cultos — "))
        }
        return listOf(
            ReportPdfSection("Escola Bíblica Dominical", sundaySchool),
            ReportPdfSection("Cultos", services),
        )
    }

    private fun isoDateToPtBr(value: String): String {
        val parts = value.trim().split('-')
        if (parts.size != 3) return value.trim()
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]
        if (year.length != 4 || month.length != 2 || day.length != 2) return value.trim()
        return "$day/$month/$year"
    }

    private fun sanitizeFilePart(value: String): String {
        val normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return normalized
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .take(48)
    }
}
