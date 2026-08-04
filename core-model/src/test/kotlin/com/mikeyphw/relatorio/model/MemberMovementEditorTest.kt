package com.mikeyphw.relatorio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberMovementEditorTest {
    @Test
    fun typingDateAddsSeparatorsAndLimitsDigits() {
        assertEquals("03/08/2026", BrazilianDateInput.normalizeTyping("0308202699"))
    }

    @Test
    fun invalidCalendarDateIsRejected() {
        assertEquals(null, BrazilianDateInput.toIsoOrNull("31/02/2026"))
    }

    @Test
    fun standardMovementUsesReasonAsDefaultOccurrence() {
        val movement = MemberMovementValidator.toMovement(
            input = MemberMovementInput(
                memberName = "João da Silva",
                eventDateText = "03/08/2026",
                reasonCode = "BNA",
            ),
            includesBirthDate = true,
            id = "movement-1",
        )

        assertEquals("2026-08-03", movement.eventDateIso)
        assertEquals("Batismo nas águas", movement.occurrence)
        assertEquals("", movement.birthDateIso)
    }

    @Test
    fun completeTemplateDiscardsOccurrenceAndBirthDate() {
        val movement = MemberMovementValidator.toMovement(
            input = MemberMovementInput(
                memberName = "Maria",
                eventDateText = "04/08/2026",
                reasonCode = "CMR",
                occurrence = "Texto livre",
                birthDateText = "01/01/1980",
            ),
            includesBirthDate = false,
            id = "movement-2",
        )

        assertEquals("", movement.occurrence)
        assertEquals("", movement.birthDateIso)
    }

    @Test
    fun blankNameProducesPlainLanguageMessage() {
        val messages = MemberMovementValidator.validate(
            input = MemberMovementInput(eventDateText = "03/08/2026"),
            includesBirthDate = true,
        )

        assertTrue(messages.contains("Informe o nome da pessoa."))
    }
}
