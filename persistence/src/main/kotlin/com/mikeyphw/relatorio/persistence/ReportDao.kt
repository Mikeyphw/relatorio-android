package com.mikeyphw.relatorio.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ReportDao {
    @Transaction
    @Query(
        """
        SELECT * FROM reports
        WHERE status = :draftStatus
        ORDER BY updatedAtEpochMillis DESC
        LIMIT 1
        """,
    )
    abstract fun observeDraft(draftStatus: String): Flow<ReportAggregate?>

    @Query(
        """
        SELECT * FROM reports
        WHERE status = :completedStatus
        ORDER BY year DESC, month DESC, updatedAtEpochMillis DESC
        """,
    )
    abstract fun observeCompletedReports(completedStatus: String): Flow<List<ReportEntity>>

    @Transaction
    @Query("SELECT * FROM reports WHERE id = :reportId LIMIT 1")
    abstract suspend fun getReport(reportId: String): ReportAggregate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReport(report: ReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertValues(values: List<ReportValueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMovements(movements: List<MemberMovementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMovement(movement: MemberMovementEntity)

    @Query("DELETE FROM reports WHERE status = :status")
    abstract suspend fun deleteReportsByStatus(status: String)

    @Query("DELETE FROM reports WHERE status = :draftStatus AND id != :reportId")
    abstract suspend fun deleteOtherDrafts(draftStatus: String, reportId: String)

    @Query("DELETE FROM reports WHERE id = :reportId")
    abstract suspend fun deleteReport(reportId: String)

    @Query(
        """
        DELETE FROM reports
        WHERE id = :reportId AND status = :completedStatus
        """,
    )
    abstract suspend fun deleteCompletedReport(reportId: String, completedStatus: String): Int

    @Query(
        """
        DELETE FROM member_movements
        WHERE id = :movementId AND reportId = :reportId
        """,
    )
    abstract suspend fun deleteMovement(reportId: String, movementId: String): Int

    @Query(
        """
        UPDATE reports SET
            month = :month,
            year = :year,
            regional = :regional,
            congregation = :congregation,
            leadPastor = :leadPastor,
            ministry = :ministry,
            presidentPastor = :presidentPastor,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :reportId
        """,
    )
    abstract suspend fun updateIdentity(
        reportId: String,
        month: Int,
        year: Int,
        regional: String,
        congregation: String,
        leadPastor: String,
        ministry: String,
        presidentPastor: String,
        updatedAtEpochMillis: Long,
    )

    @Query(
        """
        UPDATE reports SET
            status = :status,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :reportId
        """,
    )
    abstract suspend fun updateStatus(
        reportId: String,
        status: String,
        updatedAtEpochMillis: Long,
    )


    @Query(
        """
        UPDATE reports SET
            status = :draftStatus,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :reportId AND status = :completedStatus
        """,
    )
    abstract suspend fun reopenCompletedReport(
        reportId: String,
        completedStatus: String,
        draftStatus: String,
        updatedAtEpochMillis: Long,
    ): Int

    @Query(
        """
        UPDATE reports SET
            notes = :notes,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :reportId
        """,
    )
    abstract suspend fun updateNotes(
        reportId: String,
        notes: String,
        updatedAtEpochMillis: Long,
    ): Int

    @Query(
        """
        UPDATE report_values SET value = :value
        WHERE reportId = :reportId AND fieldId = :fieldId
        """,
    )
    abstract suspend fun updateValue(
        reportId: String,
        fieldId: String,
        value: Int,
    ): Int

    @Query(
        """
        UPDATE report_values SET customLabel = :customLabel
        WHERE reportId = :reportId AND fieldId = :fieldId
        """,
    )
    abstract suspend fun updateCustomLabel(
        reportId: String,
        fieldId: String,
        customLabel: String?,
    ): Int

    @Query(
        """
        UPDATE reports SET updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :reportId
        """,
    )
    abstract suspend fun touchReport(
        reportId: String,
        updatedAtEpochMillis: Long,
    )

    @Transaction
    open suspend fun replaceDraft(
        draftStatus: String,
        report: ReportEntity,
        values: List<ReportValueEntity>,
        movements: List<MemberMovementEntity>,
    ) {
        deleteReportsByStatus(draftStatus)
        insertReport(report)
        insertValues(values)
        if (movements.isNotEmpty()) insertMovements(movements)
    }

    @Transaction
    open suspend fun updateDraftValue(
        reportId: String,
        fieldId: String,
        value: Int,
        updatedAtEpochMillis: Long,
    ) {
        check(updateValue(reportId, fieldId, value) == 1) {
            "Campo de relatório não encontrado: $fieldId"
        }
        touchReport(reportId, updatedAtEpochMillis)
    }

    @Transaction
    open suspend fun updateDraftCustomLabel(
        reportId: String,
        fieldId: String,
        customLabel: String?,
        updatedAtEpochMillis: Long,
    ) {
        check(updateCustomLabel(reportId, fieldId, customLabel) == 1) {
            "Campo de relatório não encontrado: $fieldId"
        }
        touchReport(reportId, updatedAtEpochMillis)
    }

    @Transaction
    open suspend fun saveDraftMovement(
        reportId: String,
        movement: MemberMovementEntity,
        updatedAtEpochMillis: Long,
    ) {
        check(movement.reportId == reportId) { "Movimentação ligada a outro relatório." }
        insertMovement(movement)
        touchReport(reportId, updatedAtEpochMillis)
    }

    @Transaction
    open suspend fun deleteDraftMovement(
        reportId: String,
        movementId: String,
        updatedAtEpochMillis: Long,
    ) {
        check(deleteMovement(reportId, movementId) == 1) {
            "Movimentação não encontrada: $movementId"
        }
        touchReport(reportId, updatedAtEpochMillis)
    }
    @Transaction
    open suspend fun reopenAsOnlyDraft(
        reportId: String,
        completedStatus: String,
        draftStatus: String,
        updatedAtEpochMillis: Long,
    ) {
        deleteOtherDrafts(draftStatus = draftStatus, reportId = reportId)
        check(
            reopenCompletedReport(
                reportId = reportId,
                completedStatus = completedStatus,
                draftStatus = draftStatus,
                updatedAtEpochMillis = updatedAtEpochMillis,
            ) == 1,
        ) { "Relatório concluído não encontrado: $reportId" }
    }

}
