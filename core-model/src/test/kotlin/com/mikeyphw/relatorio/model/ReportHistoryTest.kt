package com.mikeyphw.relatorio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportHistoryTest {
    @Test
    fun `copy for next month keeps values and clears monthly events`() {
        val source = report(month = 12, year = 2026)
        val copy = ReportHistory.copyForNextMonth(
            source = source,
            nowEpochMillis = 99L,
            newId = "copy-id",
        )

        assertEquals("copy-id", copy.id)
        assertEquals(1, copy.identity.month)
        assertEquals(2027, copy.identity.year)
        assertEquals(ReportStatus.Draft, copy.status)
        assertEquals(source.values, copy.values)
        assertTrue(copy.movements.isEmpty())
        assertEquals("", copy.notes)
        assertEquals(99L, copy.createdAtEpochMillis)
    }


    @Test
    fun `copy for selected month keeps values and clears monthly events`() {
        val source = report(month = 6, year = 2026)
        val copy = ReportHistory.copyForMonth(
            source = source,
            month = 7,
            year = 2026,
            nowEpochMillis = 123L,
            newId = "selected-month-copy",
        )

        assertEquals("selected-month-copy", copy.id)
        assertEquals(7, copy.identity.month)
        assertEquals(2026, copy.identity.year)
        assertEquals(source.values, copy.values)
        assertTrue(copy.movements.isEmpty())
        assertEquals("", copy.notes)
        assertEquals(123L, copy.createdAtEpochMillis)
    }

    @Test
    fun `filter matches congregation month year and template`() {
        val reports = listOf(
            summary("1", "Primavera VI", 8, 2026, ReportTemplateId.AdTimoteo2026),
            summary("2", "Centro", 1, 2025, ReportTemplateId.Complete),
        )

        assertEquals(listOf("1"), ReportHistory.filter(reports, "primavera").map { it.id })
        assertEquals(listOf("1"), ReportHistory.filter(reports, "agosto").map { it.id })
        assertEquals(listOf("1"), ReportHistory.filter(reports, "relatorio ad").map { it.id })
        assertEquals(listOf("2"), ReportHistory.filter(reports, "", year = 2025).map { it.id })
        assertEquals(
            listOf("2"),
            ReportHistory.filter(reports, "", templateId = ReportTemplateId.Complete).map { it.id },
        )
    }

    private fun report(month: Int, year: Int) = MonthlyReport(
        id = "source",
        templateId = ReportTemplateId.AdTimoteo2026,
        status = ReportStatus.Completed,
        identity = ReportIdentity(month = month, year = year, congregation = "Primavera VI"),
        values = listOf(ReportValue("membros.membros", 120, "Membros")),
        movements = listOf(MemberMovement(memberName = "João", reasonCode = "BNA")),
        notes = "Anotação mensal",
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 2L,
    )

    private fun summary(
        id: String,
        congregation: String,
        month: Int,
        year: Int,
        templateId: ReportTemplateId,
    ) = ReportSummary(
        id = id,
        templateId = templateId,
        status = ReportStatus.Completed,
        month = month,
        year = year,
        congregation = congregation,
        updatedAtEpochMillis = 1L,
    )
}
