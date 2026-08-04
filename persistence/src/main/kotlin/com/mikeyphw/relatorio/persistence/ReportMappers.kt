package com.mikeyphw.relatorio.persistence

import com.mikeyphw.relatorio.model.MemberMovement
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportStatus
import com.mikeyphw.relatorio.model.ReportSummary
import com.mikeyphw.relatorio.model.ReportTemplateId
import com.mikeyphw.relatorio.model.ReportValue

internal fun MonthlyReport.toEntity() = ReportEntity(
    id = id,
    templateId = templateId.stableId,
    status = status.storageValue,
    month = identity.month,
    year = identity.year,
    regional = identity.regional,
    congregation = identity.congregation,
    leadPastor = identity.leadPastor,
    ministry = identity.ministry,
    presidentPastor = identity.presidentPastor,
    notes = notes,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun MonthlyReport.valueEntities() = values.map { value ->
    ReportValueEntity(
        reportId = id,
        fieldId = value.fieldId,
        value = value.value,
        customLabel = value.customLabel,
    )
}

internal fun MemberMovement.toEntity(reportId: String) = MemberMovementEntity(
    id = id,
    reportId = reportId,
    registrationNumber = registrationNumber,
    memberName = memberName,
    eventDateIso = eventDateIso,
    reasonCode = reasonCode,
    occurrence = occurrence,
    birthDateIso = birthDateIso,
)

internal fun MonthlyReport.movementEntities() = movements.map { movement ->
    movement.toEntity(reportId = id)
}

internal fun ReportAggregate.toModel() = MonthlyReport(
    id = report.id,
    templateId = ReportTemplateId.fromStableId(report.templateId),
    status = ReportStatus.fromStorageValue(report.status),
    identity = ReportIdentity(
        month = report.month,
        year = report.year,
        regional = report.regional,
        congregation = report.congregation,
        leadPastor = report.leadPastor,
        ministry = report.ministry,
        presidentPastor = report.presidentPastor,
    ),
    values = values.sortedBy { it.fieldId }.map { value ->
        ReportValue(
            fieldId = value.fieldId,
            value = value.value,
            customLabel = value.customLabel,
        )
    },
    movements = movements.sortedBy { it.memberName.lowercase() }.map { movement ->
        MemberMovement(
            id = movement.id,
            registrationNumber = movement.registrationNumber,
            memberName = movement.memberName,
            eventDateIso = movement.eventDateIso,
            reasonCode = movement.reasonCode,
            occurrence = movement.occurrence,
            birthDateIso = movement.birthDateIso,
        )
    },
    notes = report.notes,
    createdAtEpochMillis = report.createdAtEpochMillis,
    updatedAtEpochMillis = report.updatedAtEpochMillis,
)

internal fun ReportEntity.toSummary() = ReportSummary(
    id = id,
    templateId = ReportTemplateId.fromStableId(templateId),
    status = ReportStatus.fromStorageValue(status),
    month = month,
    year = year,
    congregation = congregation,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
