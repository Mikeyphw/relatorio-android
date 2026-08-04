package com.mikeyphw.relatorio.persistence

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
}
