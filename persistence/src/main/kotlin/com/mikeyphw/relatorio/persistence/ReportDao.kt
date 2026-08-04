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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReport(report: ReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertValues(values: List<ReportValueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMovements(movements: List<MemberMovementEntity>)

    @Query("DELETE FROM reports WHERE status = :status")
    abstract suspend fun deleteReportsByStatus(status: String)

    @Query("DELETE FROM reports WHERE id = :reportId")
    abstract suspend fun deleteReport(reportId: String)

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
}
