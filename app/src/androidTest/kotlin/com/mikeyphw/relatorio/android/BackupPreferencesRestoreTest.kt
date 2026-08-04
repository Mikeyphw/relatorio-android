package com.mikeyphw.relatorio.android

import androidx.test.platform.app.InstrumentationRegistry
import com.mikeyphw.relatorio.android.data.TextSizePreference
import com.mikeyphw.relatorio.model.BackupPreferencesSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BackupPreferencesRestoreTest {
    @get:Rule
    val dataResetRule = TestAppDataResetRule()

    @Test
    fun restoresPreferencesEvenWhenNoReportIsImported() = runBlocking {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as RelatorioApplication

        application.container.preferences.restoreBackupSnapshot(
            snapshot = BackupPreferencesSnapshot(
                regional = "Regional restaurada",
                congregation = "Congregação restaurada",
                textSizePreferenceId = "extra_large",
                highContrast = true,
            ),
            restoredDraftId = null,
            preserveCurrentDraftProgress = false,
        )

        val restored = application.container.preferences.values.first()
        assertEquals("Regional restaurada", restored.regional)
        assertEquals("Congregação restaurada", restored.congregation)
        assertEquals(TextSizePreference.ExtraLarge, restored.textSizePreference)
        assertTrue(restored.highContrast)
    }
}
