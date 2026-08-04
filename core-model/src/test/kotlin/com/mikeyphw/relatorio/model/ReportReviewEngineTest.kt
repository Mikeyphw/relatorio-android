package com.mikeyphw.relatorio.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportReviewEngineTest {
    @Test
    fun `clean standard report can be completed`() {
        val report = draft().copy(
            identity = validIdentity(),
            values = draft().values.map { value ->
                when (value.fieldId) {
                    "members.members" -> value.copy(value = 10)
                    "communion.participants" -> value.copy(value = 8)
                    "communion.non_participants" -> value.copy(value = 2)
                    "sunday_school_and_services.sunday_school_total" -> value.copy(value = 10)
                    "sunday_school_and_services.sunday_school_adults" -> value.copy(value = 4)
                    "sunday_school_and_services.sunday_school_youth" -> value.copy(value = 2)
                    "sunday_school_and_services.sunday_school_teenagers" -> value.copy(value = 2)
                    "sunday_school_and_services.sunday_school_children" -> value.copy(value = 2)
                    else -> value
                }
            },
        )

        val result = ReportReviewEngine.review(report)

        assertTrue(result.canComplete)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `missing congregation blocks completion`() {
        val result = ReportReviewEngine.review(draft())

        assertFalse(result.canComplete)
        assertTrue(result.blockingIssues.any { it.id.startsWith("identity.") })
    }

    @Test
    fun `sunday school mismatch is a warning`() {
        val original = draft()
        val report = original.copy(
            identity = validIdentity(),
            values = original.values.map { value ->
                when (value.fieldId) {
                    "sunday_school_and_services.sunday_school_total" -> value.copy(value = 12)
                    "sunday_school_and_services.sunday_school_adults" -> value.copy(value = 4)
                    "sunday_school_and_services.sunday_school_youth" -> value.copy(value = 3)
                    else -> value
                }
            },
        )

        val result = ReportReviewEngine.review(report)

        assertTrue(result.canComplete)
        assertTrue(result.warningIssues.any { it.id == "consistency.sunday_school" })
    }

    @Test
    fun `movement outside report month is a warning`() {
        val report = draft().copy(
            identity = validIdentity(),
            movements = listOf(
                MemberMovement(
                    memberName = "Maria",
                    eventDateIso = "2026-07-31",
                    reasonCode = "BNA",
                    occurrence = "Batismo nas águas",
                ),
            ),
        )

        val result = ReportReviewEngine.review(report)

        assertTrue(result.canComplete)
        assertTrue(result.warningIssues.any { it.id.endsWith("date_outside_month") })
    }

    private fun draft() = ReportDraftFactory.create(
        templateId = ReportTemplateId.AdTimoteo2026,
        identity = ReportIdentity(month = 8, year = 2026),
        nowEpochMillis = 1L,
        id = "report",
    )

    private fun validIdentity() = ReportIdentity(
        month = 8,
        year = 2026,
        congregation = "Primavera VI",
    )
}
