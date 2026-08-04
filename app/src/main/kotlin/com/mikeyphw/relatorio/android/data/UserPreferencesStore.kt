package com.mikeyphw.relatorio.android.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportTemplateId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reportPreferencesDataStore by preferencesDataStore(name = "report_preferences")

data class UserPreferences(
    val defaultTemplateId: ReportTemplateId = ReportTemplateId.AdTimoteo2026,
    val regional: String = "",
    val congregation: String = "",
    val leadPastor: String = "",
    val ministry: String = "Ministério AD Timóteo MG",
    val presidentPastor: String = "Carmo Matias Gonçalves",
)

class UserPreferencesStore(private val context: Context) {
    val values: Flow<UserPreferences> = context.reportPreferencesDataStore.data.map(::toUserPreferences)

    suspend fun setDefaultTemplate(templateId: ReportTemplateId) {
        context.reportPreferencesDataStore.edit { preferences ->
            preferences[Keys.DefaultTemplate] = templateId.stableId
        }
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

    private fun toUserPreferences(preferences: Preferences): UserPreferences = UserPreferences(
        defaultTemplateId = ReportTemplateId.fromStableId(
            preferences[Keys.DefaultTemplate] ?: ReportTemplateId.AdTimoteo2026.stableId,
        ),
        regional = preferences[Keys.Regional].orEmpty(),
        congregation = preferences[Keys.Congregation].orEmpty(),
        leadPastor = preferences[Keys.LeadPastor].orEmpty(),
        ministry = preferences[Keys.Ministry] ?: "Ministério AD Timóteo MG",
        presidentPastor = preferences[Keys.PresidentPastor] ?: "Carmo Matias Gonçalves",
    )

    private object Keys {
        val DefaultTemplate = stringPreferencesKey("default_template")
        val Regional = stringPreferencesKey("regional")
        val Congregation = stringPreferencesKey("congregation")
        val LeadPastor = stringPreferencesKey("lead_pastor")
        val Ministry = stringPreferencesKey("ministry")
        val PresidentPastor = stringPreferencesKey("president_pastor")
    }
}
