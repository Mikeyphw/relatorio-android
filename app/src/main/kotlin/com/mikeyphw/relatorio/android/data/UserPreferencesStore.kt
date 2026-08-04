package com.mikeyphw.relatorio.android.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mikeyphw.relatorio.model.BackupPreferencesSnapshot
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportTemplateId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reportPreferencesDataStore by preferencesDataStore(name = "report_preferences")

data class DraftProgress(
    val reportId: String = "",
    val currentSectionIndex: Int = 0,
    val completedSectionCount: Int = 0,
)

enum class TextSizePreference(val stableId: String) {
    Large("large"),
    ExtraLarge("extra_large");

    companion object {
        fun fromStableId(value: String?): TextSizePreference =
            entries.firstOrNull { it.stableId == value } ?: Large
    }
}

data class UserPreferences(
    val defaultTemplateId: ReportTemplateId = ReportTemplateId.AdTimoteo2026,
    val regional: String = "",
    val congregation: String = "",
    val leadPastor: String = "",
    val ministry: String = "Ministério AD Timóteo MG",
    val presidentPastor: String = "Carmo Matias Gonçalves",
    val textSizePreference: TextSizePreference = TextSizePreference.Large,
    val highContrast: Boolean = false,
    val pdfAllTextBold: Boolean = false,
    val pdfOfficialFormLayout: Boolean = false,
    val draftProgress: DraftProgress = DraftProgress(),
)

class UserPreferencesStore(private val context: Context) {
    val values: Flow<UserPreferences> = context.reportPreferencesDataStore.data.map(::toUserPreferences)

    suspend fun setDefaultTemplate(templateId: ReportTemplateId) {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences[Keys.DefaultTemplate] = templateId.stableId
        }
    }

    suspend fun backupSnapshot(): BackupPreferencesSnapshot {
        val current = values.first()
        return BackupPreferencesSnapshot(
            defaultTemplateId = current.defaultTemplateId.stableId,
            regional = current.regional,
            congregation = current.congregation,
            leadPastor = current.leadPastor,
            ministry = current.ministry,
            presidentPastor = current.presidentPastor,
            textSizePreferenceId = current.textSizePreference.stableId,
            highContrast = current.highContrast,
            pdfAllTextBold = current.pdfAllTextBold,
            pdfOfficialFormLayout = current.pdfOfficialFormLayout,
            draftProgressReportId = current.draftProgress.reportId,
            draftProgressCurrentSection = current.draftProgress.currentSectionIndex,
            draftProgressCompletedSections = current.draftProgress.completedSectionCount,
        )
    }

    suspend fun restoreBackupSnapshot(
        snapshot: BackupPreferencesSnapshot,
        restoredDraftId: String?,
        preserveCurrentDraftProgress: Boolean,
    ) {
        context.reportPreferencesDataStore.edit { preferences ->
            val template = ReportTemplateId.entries.firstOrNull { it.stableId == snapshot.defaultTemplateId }
                ?: ReportTemplateId.AdTimoteo2026
            preferences[Keys.DefaultTemplate] = template.stableId
            preferences[Keys.Regional] = snapshot.regional.take(120)
            preferences[Keys.Congregation] = snapshot.congregation.take(160)
            preferences[Keys.LeadPastor] = snapshot.leadPastor.take(160)
            preferences[Keys.Ministry] = snapshot.ministry.take(160)
            preferences[Keys.PresidentPastor] = snapshot.presidentPastor.take(160)
            preferences[Keys.TextSize] = TextSizePreference.fromStableId(snapshot.textSizePreferenceId).stableId
            preferences[Keys.HighContrast] = snapshot.highContrast
            preferences[Keys.PdfAllTextBold] = snapshot.pdfAllTextBold
            preferences[Keys.PdfOfficialFormLayout] = snapshot.pdfOfficialFormLayout
            if (restoredDraftId != null && restoredDraftId == snapshot.draftProgressReportId) {
                preferences[Keys.DraftProgressReportId] = restoredDraftId
                preferences[Keys.DraftProgressCurrentSection] = snapshot.draftProgressCurrentSection.coerceAtLeast(0)
                preferences[Keys.DraftProgressCompletedSections] = snapshot.draftProgressCompletedSections.coerceAtLeast(0)
            } else if (!preserveCurrentDraftProgress) {
                preferences.remove(Keys.DraftProgressReportId)
                preferences.remove(Keys.DraftProgressCurrentSection)
                preferences.remove(Keys.DraftProgressCompletedSections)
            }
        }
    }

    /** Resets DataStore before an instrumentation scenario. */
    suspend fun clearAllForTesting() {
        context.reportPreferencesDataStore.edit { it.clear() }
    }

    suspend fun saveIdentity(identity: ReportIdentity) {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences[Keys.Regional] = identity.regional
            preferences[Keys.Congregation] = identity.congregation
            preferences[Keys.LeadPastor] = identity.leadPastor
            preferences[Keys.Ministry] = identity.ministry
            preferences[Keys.PresidentPastor] = identity.presidentPastor
        }
    }

    suspend fun saveHeaderDefaults(
        regional: String,
        congregation: String,
        leadPastor: String,
        ministry: String,
        presidentPastor: String,
    ) {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences[Keys.Regional] = regional.take(120)
            preferences[Keys.Congregation] = congregation.take(160)
            preferences[Keys.LeadPastor] = leadPastor.take(160)
            preferences[Keys.Ministry] = ministry.take(160)
            preferences[Keys.PresidentPastor] = presidentPastor.take(160)
        }
    }


    suspend fun setTextSizePreference(value: TextSizePreference) {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences[Keys.TextSize] = value.stableId
        }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences[Keys.HighContrast] = enabled
        }
    }

    suspend fun setPdfAllTextBold(enabled: Boolean) {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences[Keys.PdfAllTextBold] = enabled
        }
    }

    suspend fun setPdfOfficialFormLayout(enabled: Boolean) {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences[Keys.PdfOfficialFormLayout] = enabled
        }
    }

    suspend fun resetDraftProgress(reportId: String) {
        saveDraftProgress(
            reportId = reportId,
            currentSectionIndex = 0,
            completedSectionCount = 0,
        )
    }

    suspend fun clearDraftProgress() {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences.remove(Keys.DraftProgressReportId)
            preferences.remove(Keys.DraftProgressCurrentSection)
            preferences.remove(Keys.DraftProgressCompletedSections)
        }
    }

    suspend fun saveDraftProgress(
        reportId: String,
        currentSectionIndex: Int,
        completedSectionCount: Int,
    ) {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences[Keys.DraftProgressReportId] = reportId
            preferences[Keys.DraftProgressCurrentSection] = currentSectionIndex.coerceAtLeast(0)
            preferences[Keys.DraftProgressCompletedSections] = completedSectionCount.coerceAtLeast(0)
        }
    }

    private fun toUserPreferences(preferences: Preferences): UserPreferences = UserPreferences(
        defaultTemplateId = ReportTemplateId.fromStableId(
            preferences[Keys.DefaultTemplate] ?: ReportTemplateId.AdTimoteo2026.stableId,
        ),
        regional = preferences[Keys.Regional].orEmpty(),
        congregation = preferences[Keys.Congregation].orEmpty(),
        leadPastor = preferences[Keys.LeadPastor].orEmpty(),
        ministry = preferences[Keys.Ministry] ?: "Ministério AD Timóteo MG",
        presidentPastor = preferences[Keys.PresidentPastor] ?: "Carmo Matias Gonçalves",
        textSizePreference = TextSizePreference.fromStableId(preferences[Keys.TextSize]),
        highContrast = preferences[Keys.HighContrast] ?: false,
        pdfAllTextBold = preferences[Keys.PdfAllTextBold] ?: false,
        pdfOfficialFormLayout = preferences[Keys.PdfOfficialFormLayout] ?: false,
        draftProgress = DraftProgress(
            reportId = preferences[Keys.DraftProgressReportId].orEmpty(),
            currentSectionIndex = preferences[Keys.DraftProgressCurrentSection] ?: 0,
            completedSectionCount = preferences[Keys.DraftProgressCompletedSections] ?: 0,
        ),
    )

    private object Keys {
        val DefaultTemplate = stringPreferencesKey("default_template")
        val Regional = stringPreferencesKey("regional")
        val Congregation = stringPreferencesKey("congregation")
        val LeadPastor = stringPreferencesKey("lead_pastor")
        val Ministry = stringPreferencesKey("ministry")
        val PresidentPastor = stringPreferencesKey("president_pastor")
        val TextSize = stringPreferencesKey("text_size")
        val HighContrast = booleanPreferencesKey("high_contrast")
        val PdfAllTextBold = booleanPreferencesKey("pdf_all_text_bold")
        val PdfOfficialFormLayout = booleanPreferencesKey("pdf_official_form_layout")
        val DraftProgressReportId = stringPreferencesKey("draft_progress_report_id")
        val DraftProgressCurrentSection = intPreferencesKey("draft_progress_current_section")
        val DraftProgressCompletedSections = intPreferencesKey("draft_progress_completed_sections")
    }
}
