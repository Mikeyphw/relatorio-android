package com.mikeyphw.relatorio.android

import com.mikeyphw.relatorio.android.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class IdentityFormTest {
    @Test
    fun convertsTrimmedPtBrFormValuesToDomainIdentity() {
        val identity = IdentityForm(
            month = 8,
            yearText = "2026",
            regional = " Regional 1 ",
            congregation = " Primavera VI ",
            leadPastor = " Pastor ",
        ).toIdentity()

        assertEquals(2026, identity.year)
        assertEquals("Regional 1", identity.regional)
        assertEquals("Primavera VI", identity.congregation)
    }

    @Test
    fun newReportUsesPreviousMonthAndSavedHeaderDefaults() {
        val form = IdentityForm.fromPreferences(
            preferences = UserPreferences(
                regional = "Regional Leste",
                congregation = "Primavera VI",
                leadPastor = "Pr. João",
                ministry = "AD Timóteo MG",
                presidentPastor = "Carmo Matias Gonçalves",
            ),
            today = LocalDate.of(2026, 8, 4),
        )

        assertEquals(7, form.month)
        assertEquals("2026", form.yearText)
        assertEquals("Regional Leste", form.regional)
        assertEquals("Primavera VI", form.congregation)
        assertEquals("Pr. João", form.leadPastor)
        assertEquals("AD Timóteo MG", form.ministry)
    }

    @Test
    fun previousMonthRollsJanuaryBackToDecemberOfPreviousYear() {
        val form = IdentityForm.fromPreferences(
            preferences = UserPreferences(congregation = "Centro"),
            today = LocalDate.of(2027, 1, 8),
        )

        assertEquals(12, form.month)
        assertEquals("2026", form.yearText)
    }

    @Test
    fun headerDefaultsAreTrimmedBeforeSaving() {
        val defaults = HeaderDefaultsForm(
            regional = "  Regional  ",
            congregation = "  Congregação  ",
            leadPastor = "  Dirigente  ",
            ministry = "  Ministério  ",
            presidentPastor = "  Presidente  ",
        ).normalized()

        assertEquals("Regional", defaults.regional)
        assertEquals("Congregação", defaults.congregation)
        assertEquals("Dirigente", defaults.leadPastor)
        assertEquals("Ministério", defaults.ministry)
        assertEquals("Presidente", defaults.presidentPastor)
    }
}
