package com.mikeyphw.relatorio.android

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mikeyphw.relatorio.android.backup.ReportBackupManager
import com.mikeyphw.relatorio.android.data.UserPreferences
import com.mikeyphw.relatorio.android.data.TextSizePreference
import com.mikeyphw.relatorio.android.data.UserPreferencesStore
import com.mikeyphw.relatorio.model.BackupRestorePreview
import com.mikeyphw.relatorio.model.MemberMovementInput
import com.mikeyphw.relatorio.model.MemberMovementValidator
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportBackupArchive
import com.mikeyphw.relatorio.model.ReportIdentity
import com.mikeyphw.relatorio.model.ReportIdentityValidator
import com.mikeyphw.relatorio.model.ReportReviewEngine
import com.mikeyphw.relatorio.model.ReportReviewIssue
import com.mikeyphw.relatorio.model.ReportReviewSeverity
import com.mikeyphw.relatorio.model.ReportSummary
import com.mikeyphw.relatorio.model.ReportTemplateCatalog
import com.mikeyphw.relatorio.model.ReportTemplateId
import com.mikeyphw.relatorio.persistence.ReportRepository
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppRoute {
    Home,
    NewReportStart,
    TemplatePicker,
    Identity,
    DraftSummary,
    NumericSection,
    Movements,
    MovementEditor,
    Notes,
    Review,
    CompletedReports,
    CompletedReportDetail,
    AccessibilitySettings,
    BackupRestore,
    PrivacyInfo,
}

enum class IdentityMode { Create, Edit }

private val InitialPreviousMonth: LocalDate = LocalDate.now().minusMonths(1)

data class IdentityForm(
    val month: Int = InitialPreviousMonth.monthValue,
    val yearText: String = InitialPreviousMonth.year.toString(),
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

        fun fromPreferences(
            preferences: UserPreferences,
            today: LocalDate = LocalDate.now(),
        ): IdentityForm {
            val previousMonth = today.minusMonths(1)
            return IdentityForm(
                month = previousMonth.monthValue,
                yearText = previousMonth.year.toString(),
                regional = preferences.regional,
                congregation = preferences.congregation,
                leadPastor = preferences.leadPastor,
                ministry = preferences.ministry,
                presidentPastor = preferences.presidentPastor,
            )
        }
    }
}

data class HeaderDefaultsForm(
    val regional: String = "",
    val congregation: String = "",
    val leadPastor: String = "",
    val ministry: String = "Ministério AD Timóteo MG",
    val presidentPastor: String = "Carmo Matias Gonçalves",
) {
    fun normalized(): HeaderDefaultsForm = copy(
        regional = regional.trim(),
        congregation = congregation.trim(),
        leadPastor = leadPastor.trim(),
        ministry = ministry.trim(),
        presidentPastor = presidentPastor.trim(),
    )

    companion object {
        fun fromPreferences(preferences: UserPreferences) = HeaderDefaultsForm(
            regional = preferences.regional,
            congregation = preferences.congregation,
            leadPastor = preferences.leadPastor,
            ministry = preferences.ministry,
            presidentPastor = preferences.presidentPastor,
        )
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
    val selectedCompletedReport: MonthlyReport? = null,
    val defaultTemplate: ReportTemplateId = ReportTemplateId.AdTimoteo2026,
    val currentSectionIndex: Int = 0,
    val completedSectionCount: Int = 0,
    val movementForm: MemberMovementInput = MemberMovementInput(),
    val editingMovementId: String? = null,
    val notesText: String = "",
    val notesSaving: Boolean = false,
    val reviewIssues: List<ReportReviewIssue> = emptyList(),
    val canComplete: Boolean = false,
    val saving: Boolean = false,
    val textSizePreference: TextSizePreference = TextSizePreference.Large,
    val highContrast: Boolean = false,
    val pdfAllTextBold: Boolean = false,
    val pdfOfficialFormLayout: Boolean = false,
    val headerDefaults: HeaderDefaultsForm = HeaderDefaultsForm(),
    val settingsSaving: Boolean = false,
    val settingsMessage: String? = null,
    val settingsMessageIsError: Boolean = false,
    val backupBusy: Boolean = false,
    val backupMessage: String? = null,
    val backupMessageIsError: Boolean = false,
    val backupPreview: BackupRestorePreview? = null,
    val backupReplaceCurrentDraft: Boolean = false,
)

interface AppActions {
    fun startNewReport()
    fun startBlankReport()
    fun copyPreviousCompletedReport()
    fun continueDraft()
    fun openCompletedReports()
    fun openCompletedReport(reportId: String)
    fun openAccessibilitySettings()
    fun openBackupRestore()
    fun openPrivacyInfo()
    fun exportBackup(destination: Uri, includePdfs: Boolean)
    fun loadBackup(source: Uri)
    fun setBackupReplaceCurrentDraft(enabled: Boolean)
    fun restoreLoadedBackup()
    fun dismissBackupPreview()
    fun clearBackupMessage()
    fun setTextSizePreference(value: TextSizePreference)
    fun setHighContrast(enabled: Boolean)
    fun setPdfAllTextBold(enabled: Boolean)
    fun setPdfOfficialFormLayout(enabled: Boolean)
    fun saveHeaderDefaults(defaults: HeaderDefaultsForm)
    fun clearSettingsMessage()
    fun duplicateCompletedReport()
    fun reopenCompletedReport()
    fun deleteCompletedReport()
    fun chooseTemplate(templateId: ReportTemplateId)
    fun continueFromTemplatePicker()
    fun updateIdentity(transform: (IdentityForm) -> IdentityForm)
    fun saveIdentity()
    fun editDraftIdentity()
    fun startNumericForm()
    fun updateDraftValue(fieldId: String, value: Int)
    fun updateDraftCustomLabel(fieldId: String, customLabel: String)
    fun nextNumericSection()
    fun previousNumericSection()
    fun openMovements()
    fun addMovement()
    fun editMovement(movementId: String)
    fun updateMovementForm(transform: (MemberMovementInput) -> MemberMovementInput)
    fun saveMovement()
    fun deleteMovement(movementId: String)
    fun openNotes()
    fun updateNotes(notes: String)
    fun openReview()
    fun completeReport()
    fun goHome()
    fun goBack()
}

class MainViewModel(
    private val repository: ReportRepository,
    private val preferences: UserPreferencesStore,
    private val backupManager: ReportBackupManager,
) : ViewModel(), AppActions {
    private val route = MutableStateFlow(AppRoute.Home)
    private val selectedTemplate = MutableStateFlow(ReportTemplateId.AdTimoteo2026)
    private val identityForm = MutableStateFlow(IdentityForm())
    private val identityMode = MutableStateFlow(IdentityMode.Create)
    private val validationMessages = MutableStateFlow<List<String>>(emptyList())
    private val activeSectionIndex = MutableStateFlow(0)
    private val movementForm = MutableStateFlow(MemberMovementInput())
    private val editingMovementId = MutableStateFlow<String?>(null)
    private val notesText = MutableStateFlow("")
    private val notesSaving = MutableStateFlow(false)
    private val saving = MutableStateFlow(false)
    private val selectedCompletedReport = MutableStateFlow<MonthlyReport?>(null)
    private val backupUi = MutableStateFlow(BackupUiState())
    private val settingsUi = MutableStateFlow(SettingsUiState())
    private var pendingBackupArchive: ReportBackupArchive? = null
    private var notesSaveJob: Job? = null

    private val navigationCore = combine(
        route,
        selectedTemplate,
        identityForm,
        identityMode,
        validationMessages,
    ) { route, template, form, mode, messages ->
        NavigationState(
            route = route,
            template = template,
            form = form,
            mode = mode,
            messages = messages,
            sectionIndex = 0,
        )
    }

    private val navigationState = combine(
        navigationCore,
        activeSectionIndex,
    ) { navigation, sectionIndex ->
        navigation.copy(sectionIndex = sectionIndex)
    }

    private val repositoryState = combine(
        repository.observeDraft(),
        repository.observeCompletedReports(),
        preferences.values,
    ) { draft, completed, userPreferences ->
        RepositoryState(draft, completed, userPreferences)
    }

    private val editorState = combine(
        movementForm,
        editingMovementId,
        notesText,
        notesSaving,
    ) { form, movementId, notes, isNotesSaving ->
        EditorState(form, movementId, notes, isNotesSaving)
    }

    private val workspaceState = combine(editorState, backupUi, settingsUi) { editor, backup, settings ->
        WorkspaceState(editor = editor, backup = backup, settings = settings)
    }

    val uiState: StateFlow<MainUiState> = combine(
        navigationState,
        repositoryState,
        workspaceState,
        selectedCompletedReport,
        saving,
    ) { navigation, stored, workspace, completedReport, isSaving ->
        val totalSections = stored.draft
            ?.let { ReportTemplateCatalog.definition(it.templateId).sections.size }
            ?: 0
        val storedProgress = stored.userPreferences.draftProgress.takeIf { progress ->
            stored.draft != null && progress.reportId == stored.draft.id
        }
        val reviewIssues = stored.draft?.let { draft ->
            buildList {
                addAll(ReportReviewEngine.review(draft).issues)
                if ((storedProgress?.completedSectionCount ?: 0) < totalSections) {
                    add(
                        ReportReviewIssue(
                            id = "progress.numeric_sections",
                            severity = ReportReviewSeverity.Blocking,
                            titlePtBr = "Revise todas as seções numéricas",
                            messagePtBr = "Abra o preenchimento e avance por todas as $totalSections seções antes de concluir.",
                            sectionId = "numeric",
                        ),
                    )
                }
                val duplicateExists = stored.completed.any { completed ->
                    completed.templateId == draft.templateId &&
                        completed.month == draft.identity.month &&
                        completed.year == draft.identity.year &&
                        completed.congregation.trim().equals(
                            draft.identity.congregation.trim(),
                            ignoreCase = true,
                        )
                }
                if (duplicateExists) {
                    add(
                        ReportReviewIssue(
                            id = "duplicate.completed_report",
                            severity = ReportReviewSeverity.Warning,
                            titlePtBr = "Já existe um relatório deste mês",
                            messagePtBr = "Há outro relatório concluído para o mesmo formulário, mês e congregação. Confira antes de continuar.",
                            sectionId = "identity",
                        ),
                    )
                }
            }.distinctBy(ReportReviewIssue::id)
        }.orEmpty()
        MainUiState(
            route = navigation.route,
            selectedTemplate = navigation.template,
            identityForm = navigation.form,
            identityMode = navigation.mode,
            validationMessages = navigation.messages,
            draft = stored.draft,
            completedReports = stored.completed,
            selectedCompletedReport = completedReport,
            defaultTemplate = stored.userPreferences.defaultTemplateId,
            currentSectionIndex = navigation.sectionIndex.coerceIn(
                minimumValue = 0,
                maximumValue = (totalSections - 1).coerceAtLeast(0),
            ),
            completedSectionCount = storedProgress
                ?.completedSectionCount
                ?.coerceIn(0, totalSections)
                ?: 0,
            movementForm = workspace.editor.movementForm,
            editingMovementId = workspace.editor.editingMovementId,
            notesText = workspace.editor.notesText,
            notesSaving = workspace.editor.notesSaving,
            reviewIssues = reviewIssues,
            canComplete = stored.draft != null && reviewIssues.none { it.severity == ReportReviewSeverity.Blocking },
            saving = isSaving,
            textSizePreference = stored.userPreferences.textSizePreference,
            highContrast = stored.userPreferences.highContrast,
            pdfAllTextBold = stored.userPreferences.pdfAllTextBold,
            pdfOfficialFormLayout = stored.userPreferences.pdfOfficialFormLayout,
            headerDefaults = HeaderDefaultsForm.fromPreferences(stored.userPreferences),
            settingsSaving = workspace.settings.saving,
            settingsMessage = workspace.settings.message,
            settingsMessageIsError = workspace.settings.messageIsError,
            backupBusy = workspace.backup.busy,
            backupMessage = workspace.backup.message,
            backupMessageIsError = workspace.backup.messageIsError,
            backupPreview = workspace.backup.preview,
            backupReplaceCurrentDraft = workspace.backup.replaceCurrentDraft,
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
            route.value = AppRoute.NewReportStart
        }
    }

    override fun startBlankReport() {
        viewModelScope.launch {
            selectedTemplate.value = preferences.values.first().defaultTemplateId
            identityMode.value = IdentityMode.Create
            validationMessages.value = emptyList()
            route.value = AppRoute.TemplatePicker
        }
    }

    override fun copyPreviousCompletedReport() {
        if (saving.value) return
        val previousReport = uiState.value.completedReports.firstOrNull() ?: return
        val previousMonth = LocalDate.now().minusMonths(1)
        viewModelScope.launch {
            saving.value = true
            try {
                val draft = repository.duplicateCompletedForMonth(
                    reportId = previousReport.id,
                    month = previousMonth.monthValue,
                    year = previousMonth.year,
                )
                preferences.resetDraftProgress(draft.id)
                selectedTemplate.value = draft.templateId
                selectedCompletedReport.value = null
                route.value = AppRoute.DraftSummary
            } finally {
                saving.value = false
            }
        }
    }

    override fun continueDraft() {
        val draft = uiState.value.draft ?: return
        selectedTemplate.value = draft.templateId
        route.value = AppRoute.DraftSummary
    }

    override fun openCompletedReports() {
        selectedCompletedReport.value = null
        route.value = AppRoute.CompletedReports
    }

    override fun openCompletedReport(reportId: String) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            try {
                val report = repository.getReport(reportId) ?: return@launch
                selectedCompletedReport.value = report
                route.value = AppRoute.CompletedReportDetail
            } finally {
                saving.value = false
            }
        }
    }


    override fun openAccessibilitySettings() {
        settingsUi.value = SettingsUiState()
        route.value = AppRoute.AccessibilitySettings
    }

    override fun openBackupRestore() {
        backupUi.value = BackupUiState()
        pendingBackupArchive = null
        route.value = AppRoute.BackupRestore
    }

    override fun openPrivacyInfo() {
        route.value = AppRoute.PrivacyInfo
    }

    override fun exportBackup(destination: Uri, includePdfs: Boolean) {
        if (backupUi.value.busy) return
        viewModelScope.launch {
            backupUi.value = BackupUiState(busy = true)
            runCatching {
                val archive = ReportBackupArchive(
                    createdAtEpochMillis = System.currentTimeMillis(),
                    appVersion = BuildConfig.VERSION_NAME,
                    reports = repository.snapshotAllReports(),
                    preferences = preferences.backupSnapshot(),
                )
                if (includePdfs) {
                    val pdfCount = backupManager.writeBundle(destination, archive)
                    "Pacote salvo com os dados e $pdfCount PDF(s)."
                } else {
                    backupManager.writeBackup(destination, archive)
                    "Backup dos dados salvo com sucesso."
                }
            }.onSuccess { message ->
                backupUi.value = BackupUiState(message = message)
            }.onFailure { error ->
                backupUi.value = BackupUiState(
                    message = error.userMessage("Não foi possível salvar o backup."),
                    messageIsError = true,
                )
            }
        }
    }

    override fun loadBackup(source: Uri) {
        if (backupUi.value.busy) return
        viewModelScope.launch {
            backupUi.value = BackupUiState(busy = true)
            runCatching {
                val archive = backupManager.readBackup(source)
                val preview = repository.previewRestore(archive, replaceCurrentDraft = false)
                pendingBackupArchive = archive
                preview
            }.onSuccess { preview ->
                backupUi.value = BackupUiState(preview = preview)
            }.onFailure { error ->
                pendingBackupArchive = null
                backupUi.value = BackupUiState(
                    message = error.userMessage("O arquivo não pôde ser lido como backup."),
                    messageIsError = true,
                )
            }
        }
    }

    override fun setBackupReplaceCurrentDraft(enabled: Boolean) {
        val archive = pendingBackupArchive ?: return
        if (backupUi.value.busy) return
        viewModelScope.launch {
            backupUi.value = backupUi.value.copy(busy = true, replaceCurrentDraft = enabled)
            runCatching { repository.previewRestore(archive, enabled) }
                .onSuccess { preview ->
                    backupUi.value = BackupUiState(
                        preview = preview,
                        replaceCurrentDraft = enabled,
                    )
                }
                .onFailure { error ->
                    backupUi.value = BackupUiState(
                        message = error.userMessage("Não foi possível preparar a restauração."),
                        messageIsError = true,
                    )
                }
        }
    }

    override fun restoreLoadedBackup() {
        val archive = pendingBackupArchive ?: return
        val currentState = backupUi.value
        if (currentState.busy || currentState.preview?.canRestore != true) return
        viewModelScope.launch {
            backupUi.value = currentState.copy(busy = true)
            runCatching {
                val hadDraftBeforeRestore = uiState.value.draft != null
                val result = repository.restoreBackup(archive, currentState.replaceCurrentDraft)
                preferences.restoreBackupSnapshot(
                    snapshot = archive.preferences,
                    restoredDraftId = result.restoredDraftId,
                    preserveCurrentDraftProgress = hadDraftBeforeRestore && result.restoredDraftId == null,
                )
                result
            }.onSuccess { result ->
                pendingBackupArchive = null
                backupUi.value = BackupUiState(
                    message = buildString {
                        if (result.restoredReportCount > 0) {
                            append(result.restoredReportCount)
                            append(" relatório(s) restaurado(s). ")
                        }
                        if (result.restoredPreferences) {
                            append("Configurações e preferências restauradas.")
                        }
                        if (result.skippedDuplicateCount > 0) {
                            append(" ")
                            append(result.skippedDuplicateCount)
                            append(" repetido(s) foram ignorados.")
                        }
                    },
                )
            }.onFailure { error ->
                backupUi.value = currentState.copy(
                    busy = false,
                    message = error.userMessage("Não foi possível restaurar o backup."),
                    messageIsError = true,
                )
            }
        }
    }

    override fun dismissBackupPreview() {
        pendingBackupArchive = null
        backupUi.value = BackupUiState()
    }

    override fun clearBackupMessage() {
        backupUi.value = backupUi.value.copy(message = null, messageIsError = false)
    }

    override fun setTextSizePreference(value: TextSizePreference) {
        viewModelScope.launch { preferences.setTextSizePreference(value) }
    }

    override fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { preferences.setHighContrast(enabled) }
    }

    override fun setPdfAllTextBold(enabled: Boolean) {
        viewModelScope.launch { preferences.setPdfAllTextBold(enabled) }
    }

    override fun setPdfOfficialFormLayout(enabled: Boolean) {
        viewModelScope.launch { preferences.setPdfOfficialFormLayout(enabled) }
    }

    override fun saveHeaderDefaults(defaults: HeaderDefaultsForm) {
        if (settingsUi.value.saving) return
        val normalized = defaults.normalized()
        val error = when {
            normalized.ministry.isBlank() -> "Informe o ministério padrão."
            normalized.presidentPastor.isBlank() -> "Informe o pastor presidente padrão."
            else -> null
        }
        if (error != null) {
            settingsUi.value = SettingsUiState(message = error, messageIsError = true)
            return
        }
        viewModelScope.launch {
            settingsUi.value = SettingsUiState(saving = true)
            runCatching {
                preferences.saveHeaderDefaults(
                    regional = normalized.regional,
                    congregation = normalized.congregation,
                    leadPastor = normalized.leadPastor,
                    ministry = normalized.ministry,
                    presidentPastor = normalized.presidentPastor,
                )
            }.onSuccess {
                settingsUi.value = SettingsUiState(message = "Valores padrão do cabeçalho salvos.")
            }.onFailure { errorCause ->
                settingsUi.value = SettingsUiState(
                    message = errorCause.userMessage("Não foi possível salvar os valores padrão."),
                    messageIsError = true,
                )
            }
        }
    }

    override fun clearSettingsMessage() {
        settingsUi.value = settingsUi.value.copy(message = null, messageIsError = false)
    }

    override fun duplicateCompletedReport() {
        val selected = selectedCompletedReport.value ?: return
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            try {
                val draft = repository.duplicateCompletedForNextMonth(selected.id)
                preferences.resetDraftProgress(draft.id)
                selectedTemplate.value = draft.templateId
                selectedCompletedReport.value = null
                route.value = AppRoute.DraftSummary
            } finally {
                saving.value = false
            }
        }
    }

    override fun reopenCompletedReport() {
        val selected = selectedCompletedReport.value ?: return
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            try {
                val draft = repository.reopenCompletedAsDraft(selected.id)
                val sectionCount = ReportTemplateCatalog.definition(draft.templateId).sections.size
                preferences.saveDraftProgress(
                    reportId = draft.id,
                    currentSectionIndex = 0,
                    completedSectionCount = sectionCount,
                )
                selectedTemplate.value = draft.templateId
                selectedCompletedReport.value = null
                route.value = AppRoute.DraftSummary
            } finally {
                saving.value = false
            }
        }
    }

    override fun deleteCompletedReport() {
        val selected = selectedCompletedReport.value ?: return
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            try {
                repository.deleteCompletedReport(selected.id)
                selectedCompletedReport.value = null
                route.value = AppRoute.CompletedReports
            } finally {
                saving.value = false
            }
        }
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
                    val createdDraft = repository.createDraft(selectedTemplate.value, identity)
                    preferences.setDefaultTemplate(selectedTemplate.value)
                    preferences.resetDraftProgress(createdDraft.id)
                    activeSectionIndex.value = 0
                }
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

    override fun startNumericForm() {
        val draft = uiState.value.draft ?: return
        viewModelScope.launch {
            val definition = ReportTemplateCatalog.definition(draft.templateId)
            val stored = preferences.values.first().draftProgress.takeIf { it.reportId == draft.id }
            val sectionIndex = when {
                stored == null -> 0
                stored.completedSectionCount >= definition.sections.size -> 0
                else -> stored.currentSectionIndex.coerceIn(0, definition.sections.lastIndex)
            }
            activeSectionIndex.value = sectionIndex
            route.value = AppRoute.NumericSection
        }
    }

    override fun updateDraftValue(fieldId: String, value: Int) {
        val draft = uiState.value.draft ?: return
        if (fieldId !in ReportTemplateCatalog.definition(draft.templateId).fieldIds) return
        viewModelScope.launch {
            repository.updateDraftValue(
                reportId = draft.id,
                fieldId = fieldId,
                value = value.coerceIn(0, MaximumReportValue),
            )
        }
    }

    override fun updateDraftCustomLabel(fieldId: String, customLabel: String) {
        val draft = uiState.value.draft ?: return
        val definition = ReportTemplateCatalog.definition(draft.templateId)
        val canCustomize = definition.sections.any { section ->
            section.fields.any { field ->
                "${section.id}.${field.id}" == fieldId && field.allowsCustomLabel
            }
        }
        if (!canCustomize) return
        viewModelScope.launch {
            repository.updateDraftCustomLabel(
                reportId = draft.id,
                fieldId = fieldId,
                customLabel = customLabel,
            )
        }
    }

    override fun nextNumericSection() {
        val draft = uiState.value.draft ?: return
        val definition = ReportTemplateCatalog.definition(draft.templateId)
        val current = activeSectionIndex.value.coerceIn(0, definition.sections.lastIndex)
        val completed = maxOf(uiState.value.completedSectionCount, current + 1)

        if (current < definition.sections.lastIndex) {
            val next = current + 1
            activeSectionIndex.value = next
            viewModelScope.launch {
                preferences.saveDraftProgress(
                    reportId = draft.id,
                    currentSectionIndex = next,
                    completedSectionCount = completed,
                )
            }
        } else {
            viewModelScope.launch {
                preferences.saveDraftProgress(
                    reportId = draft.id,
                    currentSectionIndex = current,
                    completedSectionCount = definition.sections.size,
                )
            }
            route.value = AppRoute.DraftSummary
        }
    }

    override fun previousNumericSection() {
        val draft = uiState.value.draft ?: return
        val current = activeSectionIndex.value
        if (current <= 0) {
            route.value = AppRoute.DraftSummary
            return
        }

        val previous = current - 1
        activeSectionIndex.value = previous
        viewModelScope.launch {
            preferences.saveDraftProgress(
                reportId = draft.id,
                currentSectionIndex = previous,
                completedSectionCount = uiState.value.completedSectionCount,
            )
        }
    }

    override fun openMovements() {
        validationMessages.value = emptyList()
        route.value = AppRoute.Movements
    }

    override fun addMovement() {
        movementForm.value = MemberMovementInput()
        editingMovementId.value = null
        validationMessages.value = emptyList()
        route.value = AppRoute.MovementEditor
    }

    override fun editMovement(movementId: String) {
        val movement = uiState.value.draft?.movements?.firstOrNull { it.id == movementId } ?: return
        movementForm.value = MemberMovementValidator.fromMovement(movement)
        editingMovementId.value = movement.id
        validationMessages.value = emptyList()
        route.value = AppRoute.MovementEditor
    }

    override fun updateMovementForm(transform: (MemberMovementInput) -> MemberMovementInput) {
        movementForm.value = transform(movementForm.value)
        validationMessages.value = emptyList()
    }

    override fun saveMovement() {
        val draft = uiState.value.draft ?: return
        val includesBirthDate = ReportTemplateCatalog.definition(draft.templateId).movementIncludesBirthDate
        val form = movementForm.value
        val errors = MemberMovementValidator.validate(form, includesBirthDate)
        if (errors.isNotEmpty()) {
            validationMessages.value = errors
            return
        }

        viewModelScope.launch {
            saving.value = true
            try {
                val movement = editingMovementId.value?.let { existingId ->
                    MemberMovementValidator.toMovement(form, includesBirthDate, existingId)
                } ?: MemberMovementValidator.toMovement(form, includesBirthDate)
                repository.saveDraftMovement(draft.id, movement)
                movementForm.value = MemberMovementInput()
                editingMovementId.value = null
                validationMessages.value = emptyList()
                route.value = AppRoute.Movements
            } finally {
                saving.value = false
            }
        }
    }

    override fun deleteMovement(movementId: String) {
        val draft = uiState.value.draft ?: return
        if (draft.movements.none { it.id == movementId }) return
        viewModelScope.launch {
            repository.deleteDraftMovement(draft.id, movementId)
        }
    }

    override fun openNotes() {
        notesSaveJob?.cancel()
        notesText.value = uiState.value.draft?.notes.orEmpty()
        notesSaving.value = false
        route.value = AppRoute.Notes
    }

    override fun updateNotes(notes: String) {
        val draft = uiState.value.draft ?: return
        val safeNotes = notes.take(MaximumNotesLength)
        notesText.value = safeNotes
        notesSaveJob?.cancel()
        notesSaving.value = true
        notesSaveJob = viewModelScope.launch {
            delay(NotesSaveDelayMillis)
            try {
                repository.updateDraftNotes(draft.id, safeNotes)
            } finally {
                notesSaving.value = false
            }
        }
    }

    override fun openReview() {
        persistNotesImmediatelyIfNeeded()
        validationMessages.value = emptyList()
        route.value = AppRoute.Review
    }

    override fun completeReport() {
        val draft = uiState.value.draft ?: return
        if (!uiState.value.canComplete || saving.value) return
        viewModelScope.launch {
            saving.value = true
            try {
                repository.complete(draft.id)
                preferences.clearDraftProgress()
                selectedCompletedReport.value = repository.getReport(draft.id)
                validationMessages.value = emptyList()
                route.value = AppRoute.CompletedReportDetail
            } finally {
                saving.value = false
            }
        }
    }

    override fun goHome() {
        persistNotesImmediatelyIfNeeded()
        route.value = AppRoute.Home
    }

    override fun goBack() {
        when (route.value) {
            AppRoute.Home -> Unit
            AppRoute.NewReportStart -> route.value = AppRoute.Home
            AppRoute.TemplatePicker -> route.value = AppRoute.NewReportStart
            AppRoute.Identity -> {
                route.value = if (identityMode.value == IdentityMode.Edit) {
                    AppRoute.DraftSummary
                } else {
                    AppRoute.TemplatePicker
                }
            }
            AppRoute.DraftSummary -> route.value = AppRoute.Home
            AppRoute.NumericSection -> previousNumericSection()
            AppRoute.Movements -> route.value = AppRoute.DraftSummary
            AppRoute.MovementEditor -> {
                validationMessages.value = emptyList()
                route.value = AppRoute.Movements
            }
            AppRoute.Notes -> {
                persistNotesImmediatelyIfNeeded()
                route.value = AppRoute.DraftSummary
            }
            AppRoute.Review -> route.value = AppRoute.DraftSummary
            AppRoute.CompletedReports -> route.value = AppRoute.Home
            AppRoute.CompletedReportDetail -> {
                selectedCompletedReport.value = null
                route.value = AppRoute.CompletedReports
            }
            AppRoute.AccessibilitySettings -> route.value = AppRoute.Home
            AppRoute.BackupRestore -> {
                pendingBackupArchive = null
                backupUi.value = BackupUiState()
                route.value = AppRoute.Home
            }
            AppRoute.PrivacyInfo -> route.value = AppRoute.Home
        }
    }

    private fun persistNotesImmediatelyIfNeeded() {
        if (route.value != AppRoute.Notes) return
        val draft = uiState.value.draft ?: return
        notesSaveJob?.cancel()
        val currentNotes = notesText.value
        notesSaveJob = viewModelScope.launch {
            notesSaving.value = true
            try {
                repository.updateDraftNotes(draft.id, currentNotes)
            } finally {
                notesSaving.value = false
            }
        }
    }

    private data class NavigationState(
        val route: AppRoute,
        val template: ReportTemplateId,
        val form: IdentityForm,
        val mode: IdentityMode,
        val messages: List<String>,
        val sectionIndex: Int,
    )

    private data class RepositoryState(
        val draft: MonthlyReport?,
        val completed: List<ReportSummary>,
        val userPreferences: UserPreferences,
    )

    private data class EditorState(
        val movementForm: MemberMovementInput,
        val editingMovementId: String?,
        val notesText: String,
        val notesSaving: Boolean,
    )

    private data class BackupUiState(
        val busy: Boolean = false,
        val message: String? = null,
        val messageIsError: Boolean = false,
        val preview: BackupRestorePreview? = null,
        val replaceCurrentDraft: Boolean = false,
    )

    private data class SettingsUiState(
        val saving: Boolean = false,
        val message: String? = null,
        val messageIsError: Boolean = false,
    )

    private data class WorkspaceState(
        val editor: EditorState,
        val backup: BackupUiState,
        val settings: SettingsUiState,
    )

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            return MainViewModel(
                repository = container.repository,
                preferences = container.preferences,
                backupManager = container.backupManager,
            ) as T
        }
    }

    private companion object {
        const val MaximumReportValue = 999_999
        const val MaximumNotesLength = 4_000
        const val NotesSaveDelayMillis = 350L
    }
}


private fun Throwable.userMessage(fallback: String): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
