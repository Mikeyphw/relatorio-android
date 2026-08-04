package com.mikeyphw.relatorio.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mikeyphw.relatorio.model.MemberMovement
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportTemplateId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportRepositoryPersistenceTest {
    private lateinit var database: ReportDatabase
    private lateinit var repository: ReportRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ReportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ReportRepository(database.reportDao()) { 1_000L }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun standardDraftIsSavedAndRestoredWithAllValues() = runTest {
        repository.createDraft(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(
                month = 8,
                year = 2026,
                congregation = "Primavera VI",
            ),
        )

        val restored = repository.observeDraft().first()

        assertNotNull(restored)
        assertEquals("Primavera VI", restored?.identity?.congregation)
        assertEquals(33, restored?.values?.size)
    }

    @Test
    fun editingIdentityDoesNotRemovePreparedValues() = runTest {
        val draft = repository.createDraft(
            templateId = ReportTemplateId.Complete,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "Central"),
        )

        repository.updateDraftIdentity(
            reportId = draft.id,
            identity = draft.identity.copy(congregation = "Central II"),
        )

        val restored = repository.observeDraft().first()
        assertEquals("Central II", restored?.identity?.congregation)
        assertEquals(74, restored?.values?.size)
    }

    @Test
    fun guidedFormValueIsSavedImmediately() = runTest {
        val draft = repository.createDraft(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "Central"),
        )

        repository.updateDraftValue(
            reportId = draft.id,
            fieldId = "ministers.pastors",
            value = 4,
        )

        val restored = repository.observeDraft().first()
        val pastors = restored?.values?.single { it.fieldId == "ministers.pastors" }
        assertEquals(4, pastors?.value)
    }

    @Test
    fun customMeetingLabelIsSavedForCompleteTemplate() = runTest {
        val draft = repository.createDraft(
            templateId = ReportTemplateId.Complete,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "Central"),
        )

        repository.updateDraftCustomLabel(
            reportId = draft.id,
            fieldId = "meetings.custom_1",
            customLabel = "Reunião de famílias",
        )

        val restored = repository.observeDraft().first()
        val customMeeting = restored?.values?.single { it.fieldId == "meetings.custom_1" }
        assertEquals("Reunião de famílias", customMeeting?.customLabel)
    }

    @Test
    fun movementAndNotesAreSavedAndRestored() = runTest {
        val draft = repository.createDraft(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "Central"),
        )

        repository.saveDraftMovement(
            reportId = draft.id,
            movement = MemberMovement(
                id = "movement-1",
                registrationNumber = "42",
                memberName = "João da Silva",
                eventDateIso = "2026-08-03",
                reasonCode = "BNA",
                occurrence = "Batismo nas águas",
                birthDateIso = "1980-01-01",
            ),
        )
        repository.updateDraftNotes(draft.id, "Anotação mensal")

        val restored = repository.observeDraft().first()
        assertEquals("João da Silva", restored?.movements?.single()?.memberName)
        assertEquals("Anotação mensal", restored?.notes)
    }

    @Test
    fun movementCanBeEditedAndDeleted() = runTest {
        val draft = repository.createDraft(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "Central"),
        )
        val original = MemberMovement(
            id = "movement-2",
            memberName = "Maria",
            eventDateIso = "2026-08-04",
            reasonCode = "CMR",
        )

        repository.saveDraftMovement(draft.id, original)
        repository.saveDraftMovement(draft.id, original.copy(memberName = "Maria Souza"))
        assertEquals("Maria Souza", repository.observeDraft().first()?.movements?.single()?.memberName)

        repository.deleteDraftMovement(draft.id, original.id)
        assertEquals(0, repository.observeDraft().first()?.movements?.size)
    }

    @Test
    fun negativeValuesAreStoredAsZero() = runTest {
        val draft = repository.createDraft(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "Central"),
        )

        repository.updateDraftValue(
            reportId = draft.id,
            fieldId = "members.members",
            value = -3,
        )

        val restored = repository.observeDraft().first()
        val members = restored?.values?.single { it.fieldId == "members.members" }
        assertEquals(0, members?.value)
    }
    @Test
    fun completedReportCanBeLoadedWithValuesAndMovementsForPdf() = runTest {
        val draft = repository.createDraft(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "Central"),
        )
        repository.updateDraftValue(draft.id, "ministers.pastors", 2)
        repository.saveDraftMovement(
            draft.id,
            MemberMovement(
                id = "pdf-movement",
                memberName = "Ana",
                eventDateIso = "2026-08-05",
                reasonCode = "BNA",
            ),
        )
        repository.complete(draft.id)

        val completed = repository.getReport(draft.id)

        assertNotNull(completed)
        assertEquals(2, completed?.values?.single { it.fieldId == "ministers.pastors" }?.value)
        assertEquals("Ana", completed?.movements?.single()?.memberName)
    }

    @Test
    fun completingReportMovesItOutOfDrafts() = runTest {
        val draft = repository.createDraft(
            templateId = ReportTemplateId.AdTimoteo2026,
            identity = ReportIdentity(month = 8, year = 2026, congregation = "Central"),
        )

        repository.complete(draft.id)

        assertNull(repository.observeDraft().first())
        val completed = repository.observeCompletedReports().first()
        assertEquals(1, completed.size)
        assertEquals(draft.id, completed.single().id)
    }

}
