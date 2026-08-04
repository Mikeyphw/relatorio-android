package com.mikeyphw.relatorio.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "reports",
    indices = [
        Index(value = ["status"]),
        Index(value = ["year", "month", "congregation", "templateId"]),
    ],
)
data class ReportEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val status: String,
    val month: Int,
    val year: Int,
    val regional: String,
    val congregation: String,
    val leadPastor: String,
    val ministry: String,
    val presidentPastor: String,
    val notes: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "report_values",
    primaryKeys = ["reportId", "fieldId"],
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["reportId"])],
)
data class ReportValueEntity(
    val reportId: String,
    val fieldId: String,
    val value: Int,
    val customLabel: String?,
)

@Entity(
    tableName = "member_movements",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["reportId"])],
)
data class MemberMovementEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val registrationNumber: String,
    val memberName: String,
    val eventDateIso: String,
    val reasonCode: String,
    val occurrence: String,
    val birthDateIso: String,
)

data class ReportAggregate(
    @Embedded val report: ReportEntity,
    @Relation(parentColumn = "id", entityColumn = "reportId")
    val values: List<ReportValueEntity>,
    @Relation(parentColumn = "id", entityColumn = "reportId")
    val movements: List<MemberMovementEntity>,
)
