package com.mikeyphw.relatorio.model

import java.time.LocalDate
import java.util.UUID

object ReportDraftFactory {
    fun create(
        templateId: ReportTemplateId,
        identity: ReportIdentity = defaultIdentity(),
        nowEpochMillis: Long = System.currentTimeMillis(),
        id: String = UUID.randomUUID().toString(),
    ): MonthlyReport {
        val template = ReportTemplateCatalog.definition(templateId)
        return MonthlyReport(
            id = id,
            templateId = templateId,
            status = ReportStatus.Draft,
            identity = identity,
            values = template.fieldIds.sorted().map { fieldId -> ReportValue(fieldId, 0) },
            movements = emptyList(),
            notes = "",
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
        )
    }

    fun defaultIdentity(today: LocalDate = LocalDate.now()): ReportIdentity = ReportIdentity(
        month = today.monthValue,
        year = today.year,
    )
}

object ReportIdentityValidator {
    fun validate(identity: ReportIdentity): List<String> = buildList {
        if (identity.month !in 1..12) add("Escolha um mês válido.")
        if (identity.year !in 2000..2100) add("Informe um ano válido.")
        if (identity.congregation.isBlank()) add("Informe a congregação.")
        if (identity.ministry.isBlank()) add("Informe o ministério.")
        if (identity.presidentPastor.isBlank()) add("Informe o pastor presidente.")
    }
}
