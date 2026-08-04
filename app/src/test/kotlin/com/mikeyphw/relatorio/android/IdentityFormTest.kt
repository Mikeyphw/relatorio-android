package com.mikeyphw.relatorio.android

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
