package com.mikeyphw.relatorio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPdfContentFactoryTest {
    @Test
    fun standardReportBuildsOfficialSixSectionPdfContent() {
        val report = ReportDraftFactory.create(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(
                month = 8,
                year = 2026,
                regional = "Regional Centro",
                congregation = "Primavera VI",
                leadPastor = "Pr. Teste",
            ),
            nowEpochMillis = 1L,
        ).copy(
            values = ReportDraftFactory.create(
                templateId = ReportTemplateId.AdTimoteo2026,
                identity = ReportIdentity(8, 2026, congregation = "Primavera VI"),
                nowEpochMillis = 1L,
            ).values.map { value ->
                if (value.fieldId == "ministers.pastors") value.copy(value = 2) else value
            },
            movements = listOf(
                MemberMovement(
                    registrationNumber = "15",
                    memberName = "Maria da Silva",
                    eventDateIso = "2026-08-03",
                    reasonCode = "BNA",
                    occurrence = "",
                    birthDateIso = "1966-05-10",
                ),
            ),
        )

        val content = ReportPdfContentFactory.create(report)

        assertEquals(6, content.sections.size)
        assertEquals(33, content.sections.sumOf { it.rows.size })
        assertEquals("Agosto de 2026", content.periodPtBr)
        assertEquals(2, content.sections.first().rows.first().value)
        assertEquals("Batismo nas águas", content.movements.single().occurrence)
        assertEquals("03/08/2026", content.movements.single().eventDatePtBr)
        assertEquals("10/05/1966", content.movements.single().birthDatePtBr)
        assertTrue(content.suggestedFileName.endsWith("_2026-08.PDF"))
        assertFalse(content.suggestedFileName.contains(' '))
    }

    @Test
    fun completeReportKeepsCustomMeetingLabelAndNoBirthDateColumn() {
        val report = ReportDraftFactory.create(
            templateId = ReportTemplateId.Complete,
            identity = ReportIdentity(
                month = 1,
                year = 2027,
                congregation = "Congregação São José",
            ),
            nowEpochMillis = 2L,
        ).let { draft ->
            draft.copy(
                values = draft.values.map { value ->
                    if (value.fieldId == "meetings.custom_1") {
                        value.copy(value = 4, customLabel = "Reunião de líderes")
                    } else {
                        value
                    }
                },
            )
        }

        val content = ReportPdfContentFactory.create(report)
        val customRow = content.sections
            .first { it.titlePtBr == "Reuniões" }
            .rows
            .first { it.labelPtBr == "Reunião de líderes" }

        assertEquals(10, content.sections.size)
        assertEquals(74, content.sections.sumOf { it.rows.size })
        assertEquals(4, customRow.value)
        assertFalse(content.movementIncludesBirthDate)
        assertTrue(content.suggestedFileName.contains("CONGREGACAO_SAO_JOSE"))
    }
}
