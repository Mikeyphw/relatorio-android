package com.mikeyphw.relatorio.persistence

import com.mikeyphw.relatorio.model.MemberMovement
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportBackupArchive
import com.mikeyphw.relatorio.model.BackupRestorePreview
import com.mikeyphw.relatorio.model.BackupRestoreResult
import com.mikeyphw.relatorio.model.ReportBackupPlanner
import com.mikeyphw.relatorio.model.ReportDraftFactory
import com.mikeyphw.relatorio.model.ReportHistory
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

    suspend fun getReport(reportId: String): MonthlyReport? =
        dao.getReport(reportId)?.toModel()

    suspend fun snapshotAllReports(): List<MonthlyReport> =
        dao.getAllReports().map { it.toModel() }

    suspend fun previewRestore(
        archive: ReportBackupArchive,
        replaceCurrentDraft: Boolean,
    ): BackupRestorePreview = ReportBackupPlanner.preview(
        archive = archive,
        existingReports = snapshotAllReports(),
        replaceCurrentDraft = replaceCurrentDraft,
    )

    suspend fun restoreBackup(
        archive: ReportBackupArchive,
        replaceCurrentDraft: Boolean,
    ): BackupRestoreResult {
        val preview = previewRestore(archive, replaceCurrentDraft)
        check(preview.blockingErrors.isEmpty()) { preview.blockingErrors.joinToString(" ") }
        check(preview.importableReports.isNotEmpty()) { "Não há relatórios novos para restaurar." }
        val reports = preview.importableReports
        dao.restoreReports(
            reports = reports.map { it.toEntity() },
            values = reports.flatMap { it.valueEntities() },
            movements = reports.flatMap { it.movementEntities() },
            replaceCurrentDraft = preview.willReplaceCurrentDraft,
            draftStatus = ReportStatus.Draft.storageValue,
        )
        return BackupRestoreResult(
            restoredReportCount = reports.size,
            skippedDuplicateCount = preview.skippedDuplicateCount,
            restoredDraftId = reports.singleOrNull { it.status == ReportStatus.Draft }?.id,
        )
    }

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

    suspend fun saveDraftMovement(reportId: String, movement: MemberMovement) {
        dao.saveDraftMovement(
            reportId = reportId,
            movement = movement.toEntity(reportId),
            updatedAtEpochMillis = nowEpochMillis(),
        )
    }

    suspend fun deleteDraftMovement(reportId: String, movementId: String) {
        dao.deleteDraftMovement(
            reportId = reportId,
            movementId = movementId,
            updatedAtEpochMillis = nowEpochMillis(),
        )
    }

    suspend fun updateDraftNotes(reportId: String, notes: String) {
        check(
            dao.updateNotes(
                reportId = reportId,
                notes = notes.take(MaximumNotesLength),
                updatedAtEpochMillis = nowEpochMillis(),
            ) == 1,
        ) { "Relatório não encontrado: $reportId" }
    }

    suspend fun complete(reportId: String) {
        dao.updateStatus(
            reportId = reportId,
            status = ReportStatus.Completed.storageValue,
            updatedAtEpochMillis = nowEpochMillis(),
        )
    }

    suspend fun duplicateCompletedForNextMonth(reportId: String): MonthlyReport {
        val source = requireNotNull(getReport(reportId)) { "Relatório não encontrado: $reportId" }
        check(source.status == ReportStatus.Completed) { "Somente relatórios concluídos podem ser reutilizados." }
        val copy = ReportHistory.copyForNextMonth(
            source = source,
            nowEpochMillis = nowEpochMillis(),
        )
        dao.replaceDraft(
            draftStatus = ReportStatus.Draft.storageValue,
            report = copy.toEntity(),
            values = copy.valueEntities(),
            movements = emptyList(),
        )
        return copy
    }

    suspend fun reopenCompletedAsDraft(reportId: String): MonthlyReport {
        dao.reopenAsOnlyDraft(
            reportId = reportId,
            completedStatus = ReportStatus.Completed.storageValue,
            draftStatus = ReportStatus.Draft.storageValue,
            updatedAtEpochMillis = nowEpochMillis(),
        )
        return requireNotNull(getReport(reportId)) { "Relatório não encontrado após reabertura: $reportId" }
    }

    suspend fun deleteCompletedReport(reportId: String) {
        check(
            dao.deleteCompletedReport(
                reportId = reportId,
                completedStatus = ReportStatus.Completed.storageValue,
            ) == 1,
        ) { "Relatório concluído não encontrado: $reportId" }
    }

    suspend fun deleteReport(reportId: String) = dao.deleteReport(reportId)

    private companion object {
        const val MaximumNotesLength = 4_000
    }
}
