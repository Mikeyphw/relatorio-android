package com.mikeyphw.relatorio.model

import java.time.DateTimeException
import java.time.LocalDate

/** Severity used by the final report review. */
enum class ReportReviewSeverity {
    Blocking,
    Warning,
}

data class ReportReviewIssue(
    val id: String,
    val severity: ReportReviewSeverity,
    val titlePtBr: String,
    val messagePtBr: String,
    val sectionId: String? = null,
)

data class ReportReviewResult(
    val issues: List<ReportReviewIssue>,
) {
    val blockingIssues: List<ReportReviewIssue> = issues.filter {
        it.severity == ReportReviewSeverity.Blocking
    }
    val warningIssues: List<ReportReviewIssue> = issues.filter {
        it.severity == ReportReviewSeverity.Warning
    }
    val canComplete: Boolean = blockingIssues.isEmpty()
}

/**
 * Pure domain review used by the Android UI and unit tests.
 *
 * Zero is a valid answer for every numeric field. The review therefore checks
 * structural integrity and cross-field consistency without inventing required
 * activity counts.
 */
object ReportReviewEngine {
    fun review(report: MonthlyReport): ReportReviewResult {
        val definition = ReportTemplateCatalog.definition(report.templateId)
        val values = report.values.associateBy(ReportValue::fieldId)
        val issues = buildList {
            addIdentityIssues(report.identity)
            addValueIntegrityIssues(definition, report.values)
            addSundaySchoolIssue(report.templateId, values)
            addCommunionIssue(values)
            addTransferMovementIssues(report.templateId, values, report.movements)
            addMovementIssues(report)
        }
        return ReportReviewResult(issues.distinctBy(ReportReviewIssue::id))
    }

    private fun MutableList<ReportReviewIssue>.addIdentityIssues(identity: ReportIdentity) {
        ReportIdentityValidator.validate(identity).forEachIndexed { index, message ->
            add(
                ReportReviewIssue(
                    id = "identity.$index",
                    severity = ReportReviewSeverity.Blocking,
                    titlePtBr = "Identificação incompleta",
                    messagePtBr = message,
                    sectionId = "identity",
                ),
            )
        }
    }

    private fun MutableList<ReportReviewIssue>.addValueIntegrityIssues(
        definition: ReportTemplateDefinition,
        reportValues: List<ReportValue>,
    ) {
        val valuesById = reportValues.groupBy(ReportValue::fieldId)
        val missing = definition.fieldIds - valuesById.keys
        if (missing.isNotEmpty()) {
            add(
                ReportReviewIssue(
                    id = "values.missing",
                    severity = ReportReviewSeverity.Blocking,
                    titlePtBr = "Campos numéricos ausentes",
                    messagePtBr = "Alguns campos do formulário não foram encontrados. Volte e abra o preenchimento novamente.",
                    sectionId = "numeric",
                ),
            )
        }

        val duplicated = valuesById.filterValues { it.size > 1 }.keys
        if (duplicated.isNotEmpty()) {
            add(
                ReportReviewIssue(
                    id = "values.duplicated",
                    severity = ReportReviewSeverity.Blocking,
                    titlePtBr = "Campos numéricos duplicados",
                    messagePtBr = "O relatório contém campos repetidos e precisa ser corrigido antes da conclusão.",
                    sectionId = "numeric",
                ),
            )
        }

        if (reportValues.any { it.value < 0 }) {
            add(
                ReportReviewIssue(
                    id = "values.negative",
                    severity = ReportReviewSeverity.Blocking,
                    titlePtBr = "Quantidade inválida",
                    messagePtBr = "Use zero ou um número maior em todos os campos.",
                    sectionId = "numeric",
                ),
            )
        }
    }

    private fun MutableList<ReportReviewIssue>.addSundaySchoolIssue(
        templateId: ReportTemplateId,
        values: Map<String, ReportValue>,
    ) {
        val fieldIds = when (templateId) {
            ReportTemplateId.AdTimoteo2026 -> SundaySchoolFields(
                total = "sunday_school_and_services.sunday_school_total",
                groups = listOf(
                    "sunday_school_and_services.sunday_school_adults",
                    "sunday_school_and_services.sunday_school_youth",
                    "sunday_school_and_services.sunday_school_teenagers",
                    "sunday_school_and_services.sunday_school_children",
                ),
            )
            ReportTemplateId.Complete -> SundaySchoolFields(
                total = "sunday_school.total",
                groups = listOf(
                    "sunday_school.adults",
                    "sunday_school.youth",
                    "sunday_school.teenagers",
                    "sunday_school.children",
                ),
            )
        }
        val total = values[fieldIds.total]?.value ?: 0
        val groupSum = fieldIds.groups.sumOf { values[it]?.value ?: 0 }
        if ((total > 0 || groupSum > 0) && total != groupSum) {
            add(
                ReportReviewIssue(
                    id = "consistency.sunday_school",
                    severity = ReportReviewSeverity.Warning,
                    titlePtBr = "Confira a Escola Bíblica Dominical",
                    messagePtBr = "O total matriculado é $total, mas a soma de adultos, jovens, adolescentes e crianças é $groupSum.",
                    sectionId = if (templateId.isDefault) "sunday_school_and_services" else "sunday_school",
                ),
            )
        }
    }

    private fun MutableList<ReportReviewIssue>.addCommunionIssue(
        values: Map<String, ReportValue>,
    ) {
        val members = values["members.members"]?.value ?: 0
        val participants = values["communion.participants"]?.value ?: 0
        val nonParticipants = values["communion.non_participants"]?.value ?: 0
        val attendanceTotal = participants + nonParticipants
        if ((members > 0 || attendanceTotal > 0) && members != attendanceTotal) {
            add(
                ReportReviewIssue(
                    id = "consistency.communion",
                    severity = ReportReviewSeverity.Warning,
                    titlePtBr = "Confira a Santa Ceia",
                    messagePtBr = "O relatório tem $members membro(s), mas participantes e não participantes somam $attendanceTotal.",
                    sectionId = "communion",
                ),
            )
        }
    }

    private fun MutableList<ReportReviewIssue>.addTransferMovementIssues(
        templateId: ReportTemplateId,
        values: Map<String, ReportValue>,
        movements: List<MemberMovement>,
    ) {
        if (templateId != ReportTemplateId.AdTimoteo2026) return

        compareMovementCount(
            issueId = "consistency.transfer_received",
            title = "Confira as cartas recebidas",
            numericCount = values["members.transfer_received"]?.value ?: 0,
            movementCount = movements.count { it.reasonCode == "CMR" },
        )
        compareMovementCount(
            issueId = "consistency.transfer_sent",
            title = "Confira as cartas expedidas",
            numericCount = values["members.transfer_sent"]?.value ?: 0,
            movementCount = movements.count { it.reasonCode == "CME" },
        )
    }

    private fun MutableList<ReportReviewIssue>.compareMovementCount(
        issueId: String,
        title: String,
        numericCount: Int,
        movementCount: Int,
    ) {
        if ((numericCount > 0 || movementCount > 0) && numericCount != movementCount) {
            add(
                ReportReviewIssue(
                    id = issueId,
                    severity = ReportReviewSeverity.Warning,
                    titlePtBr = title,
                    messagePtBr = "A quantidade informada é $numericCount, mas há $movementCount movimentação(ões) correspondente(s) cadastrada(s).",
                    sectionId = "movements",
                ),
            )
        }
    }

    private fun MutableList<ReportReviewIssue>.addMovementIssues(report: MonthlyReport) {
        report.movements.forEachIndexed { index, movement ->
            val movementNumber = index + 1
            if (movement.memberName.isBlank()) {
                add(
                    ReportReviewIssue(
                        id = "movement.${movement.id}.name",
                        severity = ReportReviewSeverity.Blocking,
                        titlePtBr = "Movimentação sem nome",
                        messagePtBr = "Informe o nome da pessoa na movimentação $movementNumber.",
                        sectionId = "movements",
                    ),
                )
            }
            if (MovementReasonCatalog.byCode(movement.reasonCode) == null) {
                add(
                    ReportReviewIssue(
                        id = "movement.${movement.id}.reason",
                        severity = ReportReviewSeverity.Blocking,
                        titlePtBr = "Motivo inválido",
                        messagePtBr = "Escolha novamente o motivo da movimentação de ${movement.memberName.ifBlank { "uma pessoa" }}.",
                        sectionId = "movements",
                    ),
                )
            }

            val eventDate = parseIsoDate(movement.eventDateIso)
            if (movement.eventDateIso.isNotBlank() && eventDate == null) {
                add(
                    ReportReviewIssue(
                        id = "movement.${movement.id}.date_invalid",
                        severity = ReportReviewSeverity.Blocking,
                        titlePtBr = "Data de movimentação inválida",
                        messagePtBr = "Corrija a data de ${movement.memberName.ifBlank { "uma movimentação" }}.",
                        sectionId = "movements",
                    ),
                )
            } else if (
                eventDate != null &&
                (eventDate.monthValue != report.identity.month || eventDate.year != report.identity.year)
            ) {
                add(
                    ReportReviewIssue(
                        id = "movement.${movement.id}.date_outside_month",
                        severity = ReportReviewSeverity.Warning,
                        titlePtBr = "Data fora do mês do relatório",
                        messagePtBr = "A movimentação de ${movement.memberName} está em ${eventDate.dayOfMonth.toString().padStart(2, '0')}/${eventDate.monthValue.toString().padStart(2, '0')}/${eventDate.year}.",
                        sectionId = "movements",
                    ),
                )
            }

            val birthDate = parseIsoDate(movement.birthDateIso)
            if (movement.birthDateIso.isNotBlank() && birthDate == null) {
                add(
                    ReportReviewIssue(
                        id = "movement.${movement.id}.birth_invalid",
                        severity = ReportReviewSeverity.Blocking,
                        titlePtBr = "Data de nascimento inválida",
                        messagePtBr = "Corrija a data de nascimento de ${movement.memberName.ifBlank { "uma pessoa" }}.",
                        sectionId = "movements",
                    ),
                )
            } else if (birthDate != null && eventDate != null && birthDate.isAfter(eventDate)) {
                add(
                    ReportReviewIssue(
                        id = "movement.${movement.id}.birth_after_event",
                        severity = ReportReviewSeverity.Warning,
                        titlePtBr = "Confira a data de nascimento",
                        messagePtBr = "A data de nascimento de ${movement.memberName} está depois da data da ocorrência.",
                        sectionId = "movements",
                    ),
                )
            }
        }
    }

    private fun parseIsoDate(value: String): LocalDate? {
        if (value.isBlank()) return null
        return try {
            LocalDate.parse(value)
        } catch (_: DateTimeException) {
            null
        }
    }

    private data class SundaySchoolFields(
        val total: String,
        val groups: List<String>,
    )
}
