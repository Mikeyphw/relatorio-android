package com.mikeyphw.relatorio.persistence

import com.mikeyphw.relatorio.model.MemberMovement
import com.mikeyphw.relatorio.model.ReportDraftFactory
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportTemplateId
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportMappersTest {
    @Test
    fun standardDraftMapsToThirtyThreePersistedValues() {
        val report = ReportDraftFactory.create(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "Primavera VI"),
            nowEpochMillis = 10L,
            id = "report-1",
        )

        val entity = report.toEntity()
        val values = report.valueEntities()

        assertEquals("AD_TIMOTEO_2026_V1", entity.templateId)
        assertEquals("Primavera VI", entity.congregation)
        assertEquals(33, values.size)
        assertEquals(0, values.sumOf { it.value })
    }

    @Test
    fun movementMapperPreservesAllFields() {
        val movement = MemberMovement(
            id = "movement-1",
            registrationNumber = "10",
            memberName = "João",
            eventDateIso = "2026-08-03",
            reasonCode = "BNA",
            occurrence = "Batismo nas águas",
            birthDateIso = "1980-01-01",
        )

        val entity = movement.toEntity(reportId = "report-1")

        assertEquals("report-1", entity.reportId)
        assertEquals("João", entity.memberName)
        assertEquals("1980-01-01", entity.birthDateIso)
    }
}
