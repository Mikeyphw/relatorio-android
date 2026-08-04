package com.mikeyphw.relatorio.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mikeyphw.relatorio.android.data.UserPreferences
import com.mikeyphw.relatorio.android.data.UserPreferencesStore
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportDraftFactory
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportIdentityValidator
import com.mikeyphw.relatorio.model.ReportSummary
import com.mikeyphw.relatorio.model.ReportTemplateId
import com.mikeyphw.relatorio.persistence.ReportRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppRoute {
    Home,
    TemplatePicker,
    Identity,
    DraftSummary,
    CompletedReports,
}

enum class IdentityMode { Create, Edit }

data class IdentityForm(
    val month: Int = LocalDate.now().monthValue,
    val yearText: String = LocalDate.now().year.toString(),
    val regional: String = "",
    val congregation: String = "",
    val leadPastor: String = "",
    val ministry: String = "Ministério AD Timóteo MG",
    val presidentPastor: String = "Carmo Matias Gonçalves",
) {
    fun toIdentity(): ReportIdentity = ReportIdentity(
        month = month,
        year = yearText.toIntOrNull() ?: 0,
        regional = regional.trim(),
        congregation = congregation.trim(),
        leadPastor = leadPastor.trim(),
        ministry = ministry.trim(),
        presidentPastor = presidentPastor.trim(),
    )

    companion object {
        fun fromIdentity(identity: ReportIdentity) = IdentityForm(
            month = identity.month,
            yearText = identity.year.toString(),
            regional = identity.regional,
            congregation = identity.congregation,
            leadPastor = identity.leadPastor,
            ministry = identity.ministry,
            presidentPastor = identity.presidentPastor,
        )

        fun fromPreferences(preferences: UserPreferences): IdentityForm {
            val today = LocalDate.now()
            return IdentityForm(
                month = today.monthValue,
                yearText = today.year.toString(),
                regional = preferences.regional,
                congregation = preferences.congregation,
                leadPastor = preferences.leadPastor,
                ministry = preferences.ministry,
                presidentPastor = preferences.presidentPastor,
            )
        }
    }
}

data class MainUiState(
    val route: AppRoute = AppRoute.Home,
    val selectedTemplate: ReportTemplateId = ReportTemplateId.AdTimoteo2026,
    val identityForm: IdentityForm = IdentityForm(),
    val identityMode: IdentityMode = IdentityMode.Create,
    val validationMessages: List<String> = emptyList(),
    val draft: MonthlyReport? = null,
    val completedReports: List<ReportSummary> = emptyList(),
    val defaultTemplate: ReportTemplateId = ReportTemplateId.AdTimoteo2026,
    val saving: Boolean = false,
)

interface AppActions {
    fun startNewReport()
    fun continueDraft()
    fun openCompletedReports()
    fun chooseTemplate(templateId: ReportTemplateId)
    fun continueFromTemplatePicker()
    fun updateIdentity(transform: (IdentityForm) -> IdentityForm)
    fun saveIdentity()
    fun editDraftIdentity()
    fun goHome()
    fun goBack()
}

class MainViewModel(
    private val repository: ReportRepository,
    private val preferences: UserPreferencesStore,
) : ViewModel(), AppActions {
    private val route = MutableStateFlow(AppRoute.Home)
    private val selectedTemplate = MutableStateFlow(ReportTemplateId.AdTimoteo2026)
    private val identityForm = MutableStateFlow(IdentityForm())
    private val identityMode = MutableStateFlow(IdentityMode.Create)
    private val validationMessages = MutableStateFlow<List<String>>(emptyList())
    private val saving = MutableStateFlow(false)

    private val navigationState = combine(
        route,
        selectedTemplate,
        identityForm,
        identityMode,
        validationMessages,
    ) { route, template, form, mode, messages ->
        NavigationState(route, template, form, mode, messages)
    }

    val uiState: StateFlow<MainUiState> = combine(
        navigationState,
        repository.observeDraft(),
        repository.observeCompletedReports(),
        preferences.values,
        saving,
    ) { navigation, draft, completed, userPreferences, isSaving ->
        MainUiState(
            route = navigation.route,
            selectedTemplate = navigation.template,
            identityForm = navigation.form,
            identityMode = navigation.mode,
            validationMessages = navigation.messages,
            draft = draft,
            completedReports = completed,
            defaultTemplate = userPreferences.defaultTemplateId,
            saving = isSaving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            val userPreferences = preferences.values.first()
            selectedTemplate.value = userPreferences.defaultTemplateId
            identityForm.value = IdentityForm.fromPreferences(userPreferences)
        }
    }

    override fun startNewReport() {
        viewModelScope.launch {
            selectedTemplate.value = preferences.values.first().defaultTemplateId
            identityMode.value = IdentityMode.Create
            validationMessages.value = emptyList()
            route.value = AppRoute.TemplatePicker
        }
    }

    override fun continueDraft() {
        val draft = uiState.value.draft ?: return
        selectedTemplate.value = draft.templateId
        route.value = AppRoute.DraftSummary
    }

    override fun openCompletedReports() {
        route.value = AppRoute.CompletedReports
    }

    override fun chooseTemplate(templateId: ReportTemplateId) {
        selectedTemplate.value = templateId
    }

    override fun continueFromTemplatePicker() {
        viewModelScope.launch {
            val userPreferences = preferences.values.first()
            identityForm.value = IdentityForm.fromPreferences(userPreferences)
            identityMode.value = IdentityMode.Create
            validationMessages.value = emptyList()
            route.value = AppRoute.Identity
        }
    }

    override fun updateIdentity(transform: (IdentityForm) -> IdentityForm) {
        identityForm.value = transform(identityForm.value)
        validationMessages.value = emptyList()
    }

    override fun saveIdentity() {
        val identity = identityForm.value.toIdentity()
        val errors = ReportIdentityValidator.validate(identity)
        if (errors.isNotEmpty()) {
            validationMessages.value = errors
            return
        }

        viewModelScope.launch {
            saving.value = true
            try {
                val currentDraft = uiState.value.draft
                if (identityMode.value == IdentityMode.Edit && currentDraft != null) {
                    repository.updateDraftIdentity(currentDraft.id, identity)
                } else {
                    repository.createDraft(selectedTemplate.value, identity)
                    preferences.setDefaultTemplate(selectedTemplate.value)
                }
                preferences.saveIdentity(identity)
                validationMessages.value = emptyList()
                route.value = AppRoute.DraftSummary
            } finally {
                saving.value = false
            }
        }
    }

    override fun editDraftIdentity() {
        val draft = uiState.value.draft ?: return
        identityForm.value = IdentityForm.fromIdentity(draft.identity)
        selectedTemplate.value = draft.templateId
        identityMode.value = IdentityMode.Edit
        validationMessages.value = emptyList()
        route.value = AppRoute.Identity
    }

    override fun goHome() {
        route.value = AppRoute.Home
    }

    override fun goBack() {
        route.value = when (route.value) {
            AppRoute.Home -> AppRoute.Home
            AppRoute.TemplatePicker -> AppRoute.Home
            AppRoute.Identity -> if (identityMode.value == IdentityMode.Edit) AppRoute.DraftSummary else AppRoute.TemplatePicker
            AppRoute.DraftSummary -> AppRoute.Home
            AppRoute.CompletedReports -> AppRoute.Home
        }
    }

    private data class NavigationState(
        val route: AppRoute,
        val template: ReportTemplateId,
        val form: IdentityForm,
        val mode: IdentityMode,
        val messages: List<String>,
    )

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            return MainViewModel(
                repository = container.repository,
                preferences = container.preferences,
            ) as T
        }
    }
}
