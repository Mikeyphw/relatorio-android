package com.mikeyphw.relatorio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportDraftFactoryTest {
    @Test
    fun newStandardDraftStartsWithAllValuesAtZero() {
        val draft = ReportDraftFactory.create(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportDraftFactory.defaultIdentity(LocalDate.of(2026, 8, 3)),
            nowEpochMillis = 123L,
            id = "draft-1",
        )

        assertEquals("draft-1", draft.id)
        assertEquals(33, draft.values.size)
        assertTrue(draft.values.all { it.value == 0 })
        assertEquals(7, draft.identity.month)
        assertEquals(2026, draft.identity.year)
    }

    @Test
    fun identityValidatorExplainsMissingCongregationInPtBr() {
        val errors = ReportIdentityValidator.validate(
            ReportIdentity(month = 8, year = 2026, congregation = ""),
        )

        assertTrue(errors.contains("Informe a congregação."))
    }
}
