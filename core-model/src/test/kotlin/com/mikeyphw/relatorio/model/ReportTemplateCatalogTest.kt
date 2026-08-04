package com.mikeyphw.relatorio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTemplateCatalogTest {
    @Test
    fun currentTemplateIsDefaultAndHasThirtyThreeFields() {
        assertTrue(ReportTemplateCatalog.standard.id.isDefault)
        assertEquals("AD_TIMOTEO_2026_V1", ReportTemplateCatalog.standard.id.stableId)
        assertEquals(33, ReportTemplateCatalog.standard.fieldCount)
        assertTrue(ReportTemplateCatalog.standard.movementIncludesBirthDate)
    }

    @Test
    fun completeTemplateRemainsAvailableWithSeventyFourFields() {
        assertFalse(ReportTemplateCatalog.complete.id.isDefault)
        assertEquals("RELATORIO_COMPLETO_V1", ReportTemplateCatalog.complete.id.stableId)
        assertEquals(74, ReportTemplateCatalog.complete.fieldCount)
        assertFalse(ReportTemplateCatalog.complete.movementIncludesBirthDate)
    }

    @Test
    fun everyFieldHasAStableUniqueIdentifierInsideItsTemplate() {
        ReportTemplateCatalog.all.forEach { template ->
            assertEquals(template.fieldCount, template.fieldIds.size)
        }
    }
}
