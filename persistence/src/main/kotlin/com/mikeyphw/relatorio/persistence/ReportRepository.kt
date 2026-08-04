package com.mikeyphw.relatorio.persistence

import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportDraftFactory
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportStatus
import com.mikeyphw.relatorio.model.ReportSummary
import com.mikeyphw.relatorio.model.ReportTemplateId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReportRepository(
    private val dao: ReportDao,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeDraft(): Flow<MonthlyReport?> =
        dao.observeDraft(ReportStatus.Draft.storageValue).map { it?.toModel() }

    fun observeCompletedReports(): Flow<List<ReportSummary>> =
        dao.observeCompletedReports(ReportStatus.Completed.storageValue)
            .map { reports -> reports.map { it.toSummary() } }

    suspend fun createDraft(
        templateId: ReportTemplateId,
        identity: ReportIdentity,
    ): MonthlyReport {
        val report = ReportDraftFactory.create(
            templateId = templateId,
            identity = identity,
            nowEpochMillis = nowEpochMillis(),
        )
        dao.replaceDraft(
            draftStatus = ReportStatus.Draft.storageValue,
            report = report.toEntity(),
            values = report.valueEntities(),
            movements = report.movementEntities(),
        )
        return report
    }

    suspend fun updateDraftIdentity(reportId: String, identity: ReportIdentity) {
        dao.updateIdentity(
            reportId = reportId,
            month = identity.month,
            year = identity.year,
            regional = identity.regional,
            congregation = identity.congregation,
            leadPastor = identity.leadPastor,
            ministry = identity.ministry,
            presidentPastor = identity.presidentPastor,
            updatedAtEpochMillis = nowEpochMillis(),
        )
    }

    suspend fun updateDraftValue(
        reportId: String,
        fieldId: String,
        value: Int,
    ) {
        dao.updateDraftValue(
            reportId = reportId,
            fieldId = fieldId,
            value = value.coerceAtLeast(0),
            updatedAtEpochMillis = nowEpochMillis(),
        )
    }

    suspend fun updateDraftCustomLabel(
        reportId: String,
        fieldId: String,
        customLabel: String,
    ) {
        dao.updateDraftCustomLabel(
            reportId = reportId,
            fieldId = fieldId,
            customLabel = customLabel.trim().take(80).ifBlank { null },
            updatedAtEpochMillis = nowEpochMillis(),
        )
    }

    suspend fun complete(reportId: String) {
        dao.updateStatus(
            reportId = reportId,
            status = ReportStatus.Completed.storageValue,
            updatedAtEpochMillis = nowEpochMillis(),
        )
    }

    suspend fun deleteReport(reportId: String) = dao.deleteReport(reportId)
}
