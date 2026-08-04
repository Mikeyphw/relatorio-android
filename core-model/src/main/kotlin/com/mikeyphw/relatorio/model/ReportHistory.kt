package com.mikeyphw.relatorio.model

import java.text.Normalizer
import java.util.UUID

/** Pure helpers for history search and safe report reuse. */
object ReportHistory {
    private val monthNames = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
    )

    fun filter(
        reports: List<ReportSummary>,
        query: String,
        year: Int? = null,
        templateId: ReportTemplateId? = null,
    ): List<ReportSummary> {
        val normalizedQuery = normalize(query)
        return reports.filter { report ->
            val monthName = monthNames.getOrElse(report.month - 1) { report.month.toString() }
            val matchesQuery = normalizedQuery.isBlank() || listOf(
                report.congregation,
                report.year.toString(),
                monthName,
                report.templateId.displayNamePtBr,
            ).any { candidate -> normalize(candidate).contains(normalizedQuery) }
            val matchesYear = year == null || report.year == year
            val matchesTemplate = templateId == null || report.templateId == templateId
            matchesQuery && matchesYear && matchesTemplate
        }
    }

    fun copyForNextMonth(
        source: MonthlyReport,
        nowEpochMillis: Long,
        newId: String = UUID.randomUUID().toString(),
    ): MonthlyReport {
        val nextMonth = if (source.identity.month == 12) 1 else source.identity.month + 1
        val nextYear = if (source.identity.month == 12) source.identity.year + 1 else source.identity.year
        return source.copy(
            id = newId,
            status = ReportStatus.Draft,
            identity = source.identity.copy(month = nextMonth, year = nextYear),
            values = source.values.map { it.copy() },
            movements = emptyList(),
            notes = "",
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
        )
    }
    private fun normalize(value: String): String = Normalizer
        .normalize(value.trim().lowercase(), Normalizer.Form.NFD)
        .replace(CombiningMarks, "")

    private val CombiningMarks = Regex("\\p{M}+")

}
