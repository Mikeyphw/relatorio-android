package com.mikeyphw.relatorio.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportTemplateId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
