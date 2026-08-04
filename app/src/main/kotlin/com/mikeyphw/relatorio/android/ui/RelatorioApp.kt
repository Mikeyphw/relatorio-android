package com.mikeyphw.relatorio.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mikeyphw.relatorio.android.AppActions
import com.mikeyphw.relatorio.android.AppRoute
import com.mikeyphw.relatorio.android.IdentityForm
import com.mikeyphw.relatorio.android.IdentityMode
import com.mikeyphw.relatorio.android.MainUiState
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportHistory
import com.mikeyphw.relatorio.model.ReportFieldDefinition
import com.mikeyphw.relatorio.model.ReportSectionDefinition
import com.mikeyphw.relatorio.model.ReportSummary
import com.mikeyphw.relatorio.model.ReportValue
import com.mikeyphw.relatorio.model.ReportTemplateCatalog
import com.mikeyphw.relatorio.model.ReportTemplateId

private val ScreenPadding = 20.dp
private val SectionGap = 20.dp
private val MinimumActionHeight = 60.dp
private val MonthNames = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioApp(
    state: MainUiState,
    actions: AppActions,
) {
    BackHandler(enabled = state.route != AppRoute.Home) { actions.goBack() }

    Scaffold(
        topBar = {
            if (state.route != AppRoute.Home) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (state.route) {
                                AppRoute.TemplatePicker -> "Novo relatório"
                                AppRoute.Identity -> "Identificação"
                                AppRoute.DraftSummary -> "Relatório em andamento"
                                AppRoute.NumericSection -> "Preenchimento"
                                AppRoute.Movements -> "Movimentações"
                                AppRoute.MovementEditor -> "Dados da pessoa"
                                AppRoute.Notes -> "Anotações"
                                AppRoute.Review -> "Revisão final"
                                AppRoute.CompletedReports -> "Relatórios prontos"
                                AppRoute.CompletedReportDetail -> "PDF do relatório"
                                AppRoute.AccessibilitySettings -> "Leitura e ajuda"
                                AppRoute.BackupRestore -> "Backup e restauração"
                                AppRoute.PrivacyInfo -> "Privacidade"
                                AppRoute.Home -> "Relatório Mensal AD"
                            },
                        )
                    },
                    navigationIcon = {
                        TextButton(onClick = actions::goBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Voltar")
                        }
                    },
                )
            }
        },
    ) { contentPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (state.route) {
                AppRoute.Home -> HomeScreen(
                    hasDraft = state.draft != null,
                    completedCount = state.completedReports.size,
                    onNewReport = actions::startNewReport,
                    onContinue = actions::continueDraft,
                    onCompleted = actions::openCompletedReports,
                    onAccessibility = actions::openAccessibilitySettings,
                    onBackup = actions::openBackupRestore,
                    onPrivacy = actions::openPrivacyInfo,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.TemplatePicker -> TemplatePickerScreen(
                    selectedTemplate = state.selectedTemplate,
                    onSelect = actions::chooseTemplate,
                    onContinue = actions::continueFromTemplatePicker,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.Identity -> IdentityScreen(
                    form = state.identityForm,
                    mode = state.identityMode,
                    messages = state.validationMessages,
                    saving = state.saving,
                    onFormChange = actions::updateIdentity,
                    onSave = actions::saveIdentity,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.DraftSummary -> DraftSummaryScreen(
                    draft = state.draft,
                    completedSectionCount = state.completedSectionCount,
                    onEditIdentity = actions::editDraftIdentity,
                    onStartFilling = actions::startNumericForm,
                    onMovements = actions::openMovements,
                    onNotes = actions::openNotes,
                    onReview = actions::openReview,
                    onHome = actions::goHome,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.NumericSection -> NumericSectionScreen(
                    draft = state.draft,
                    sectionIndex = state.currentSectionIndex,
                    completedSectionCount = state.completedSectionCount,
                    onValueChange = actions::updateDraftValue,
                    onCustomLabelChange = actions::updateDraftCustomLabel,
                    onPrevious = actions::previousNumericSection,
                    onNext = actions::nextNumericSection,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.Movements -> MovementsScreen(
                    draft = state.draft,
                    onAdd = actions::addMovement,
                    onEdit = actions::editMovement,
                    onDelete = actions::deleteMovement,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.MovementEditor -> MovementEditorScreen(
                    form = state.movementForm,
                    includesBirthDate = state.draft
                        ?.let { ReportTemplateCatalog.definition(it.templateId).movementIncludesBirthDate }
                        ?: true,
                    isEditing = state.editingMovementId != null,
                    validationMessages = state.validationMessages,
                    saving = state.saving,
                    onFormChange = actions::updateMovementForm,
                    onSave = actions::saveMovement,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.Notes -> NotesScreen(
                    notes = state.notesText,
                    saving = state.notesSaving,
                    onNotesChange = actions::updateNotes,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.Review -> ReviewScreen(
                    draft = state.draft,
                    completedSectionCount = state.completedSectionCount,
                    issues = state.reviewIssues,
                    canComplete = state.canComplete,
                    saving = state.saving,
                    onComplete = actions::completeReport,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.CompletedReports -> CompletedReportsScreen(
                    reports = state.completedReports,
                    onOpenReport = actions::openCompletedReport,
                    onHome = actions::goHome,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.CompletedReportDetail -> CompletedReportDetailScreen(
                    report = state.selectedCompletedReport,
                    hasDraft = state.draft != null,
                    saving = state.saving,
                    onDuplicate = actions::duplicateCompletedReport,
                    onReopen = actions::reopenCompletedReport,
                    onDelete = actions::deleteCompletedReport,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.AccessibilitySettings -> AccessibilitySettingsScreen(
                    textSizePreference = state.textSizePreference,
                    highContrast = state.highContrast,
                    onTextSizeChange = actions::setTextSizePreference,
                    onHighContrastChange = actions::setHighContrast,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.BackupRestore -> BackupRestoreScreen(
                    hasCurrentDraft = state.draft != null,
                    busy = state.backupBusy,
                    message = state.backupMessage,
                    messageIsError = state.backupMessageIsError,
                    preview = state.backupPreview,
                    replaceCurrentDraft = state.backupReplaceCurrentDraft,
                    onExport = actions::exportBackup,
                    onLoad = actions::loadBackup,
                    onReplaceCurrentDraftChange = actions::setBackupReplaceCurrentDraft,
                    onRestore = actions::restoreLoadedBackup,
                    onDismissPreview = actions::dismissBackupPreview,
                    onClearMessage = actions::clearBackupMessage,
                    modifier = Modifier.padding(contentPadding),
                )
                AppRoute.PrivacyInfo -> PrivacyInfoScreen(
                    modifier = Modifier.padding(contentPadding),
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    hasDraft: Boolean,
    completedCount: Int,
    onNewReport: () -> Unit,
    onContinue: () -> Unit,
    onCompleted: () -> Unit,
    onAccessibility: () -> Unit,
    onBackup: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showReplaceDraftConfirmation by remember(hasDraft) { mutableStateOf(false) }

    if (showReplaceDraftConfirmation) {
        AlertDialog(
            onDismissRequest = { showReplaceDraftConfirmation = false },
            title = { Text("Já existe um relatório em andamento") },
            text = { Text("Começar outro relatório substituirá o rascunho atual somente depois que os novos dados forem salvos.") },
            confirmButton = {
                Button(
                    onClick = {
                        showReplaceDraftConfirmation = false
                        onNewReport()
                    },
                ) { Text("Começar outro") }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDraftConfirmation = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        Spacer(Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Default.Article,
            contentDescription = null,
            modifier = Modifier.size(58.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Relatório Mensal AD",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Preencha com calma. O aplicativo guarda o relatório automaticamente.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LargeActionButton(
            text = "Preencher novo relatório",
            icon = Icons.Default.Description,
            onClick = {
                if (hasDraft) showReplaceDraftConfirmation = true else onNewReport()
            },
        )
        LargeActionButton(
            text = "Continuar relatório",
            supportingText = if (hasDraft) "Há um rascunho salvo" else "Nenhum rascunho salvo",
            icon = Icons.Default.Edit,
            onClick = onContinue,
            enabled = hasDraft,
            outlined = true,
        )
        LargeActionButton(
            text = "Relatórios prontos",
            supportingText = if (completedCount == 0) "Nenhum relatório concluído" else "$completedCount relatório(s)",
            icon = Icons.Default.FolderOpen,
            onClick = onCompleted,
            outlined = true,
        )
        LargeActionButton(
            text = "Leitura e ajuda",
            supportingText = "Aumentar letras, reforçar contraste e ver instruções",
            icon = Icons.Default.AccessibilityNew,
            onClick = onAccessibility,
            outlined = true,
        )
        LargeActionButton(
            text = "Backup e restauração",
            supportingText = "Guardar os dados ou levar para outro celular",
            icon = Icons.Default.FolderOpen,
            onClick = onBackup,
            outlined = true,
        )
        LargeActionButton(
            text = "Privacidade e informações",
            supportingText = "Como os dados são guardados e guia rápido",
            icon = Icons.Default.Article,
            onClick = onPrivacy,
            outlined = true,
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Formulário padrão", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Relatório AD Timóteo, modelo atual de 2026.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TemplatePickerScreen(
    selectedTemplate: ReportTemplateId,
    onSelect: (ReportTemplateId) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        Text(
            "Escolha o formulário",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "O formulário atual já está selecionado.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReportTemplateId.entries.forEach { templateId ->
                TemplateChoiceCard(
                    templateId = templateId,
                    selected = selectedTemplate == templateId,
                    onClick = { onSelect(templateId) },
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Text(
                text = "Use o Relatório Completo somente quando sua congregação precisar do formulário anterior.",
                modifier = Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinimumActionHeight),
        ) {
            Text("Continuar")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.NavigateNext, contentDescription = null)
        }
    }
}

@Composable
private fun TemplateChoiceCard(
    templateId: ReportTemplateId,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val definition = ReportTemplateCatalog.definition(templateId)
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        border = BorderStroke(
            width = if (selected) 3.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(templateId.displayNamePtBr, style = MaterialTheme.typography.titleLarge)
                Text(
                    templateId.descriptionPtBr,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${definition.fieldCount} campos numéricos",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (templateId.isDefault) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            "Padrão",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityScreen(
    form: IdentityForm,
    mode: IdentityMode,
    messages: List<String>,
    saving: Boolean,
    onFormChange: ((IdentityForm) -> IdentityForm) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = if (mode == IdentityMode.Edit) "Editar identificação" else "Dados do relatório",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Confira os dados. As informações da igreja serão lembradas no próximo mês.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        MonthPicker(
            selectedMonth = form.month,
            onMonthSelected = { month -> onFormChange { it.copy(month = month) } },
        )
        OutlinedTextField(
            value = form.yearText,
            onValueChange = { value -> onFormChange { it.copy(yearText = value.filter(Char::isDigit).take(4)) } },
            label = { Text("Ano") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        IdentityTextField("Regional", form.regional) { value -> onFormChange { it.copy(regional = value) } }
        IdentityTextField("Congregação", form.congregation) { value -> onFormChange { it.copy(congregation = value) } }
        IdentityTextField("Pastor dirigente", form.leadPastor) { value -> onFormChange { it.copy(leadPastor = value) } }

        HorizontalDivider()
        Text("Dados que serão repetidos", style = MaterialTheme.typography.titleLarge)
        IdentityTextField("Ministério", form.ministry) { value -> onFormChange { it.copy(ministry = value) } }
        IdentityTextField("Pastor presidente", form.presidentPastor) { value -> onFormChange { it.copy(presidentPastor = value) } }

        messages.forEach { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Button(
            enabled = !saving,
            onClick = {
                focusManager.clearFocus()
                onSave()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinimumActionHeight),
        ) {
            Text(if (saving) "Salvando..." else "Salvar e continuar")
        }
    }
}

@Composable
private fun IdentityTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MonthPicker(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mês", style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinimumActionHeight),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Text(MonthNames.getOrElse(selectedMonth - 1) { "Escolher mês" })
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                MonthNames.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onMonthSelected(index + 1)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftSummaryScreen(
    draft: MonthlyReport?,
    completedSectionCount: Int,
    onEditIdentity: () -> Unit,
    onStartFilling: () -> Unit,
    onMovements: () -> Unit,
    onNotes: () -> Unit,
    onReview: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        if (draft == null) {
            Text("Carregando o rascunho...", style = MaterialTheme.typography.titleLarge)
            return@Column
        }

        val definition = ReportTemplateCatalog.definition(draft.templateId)
        val totalSections = definition.sections.size
        val completed = completedSectionCount.coerceIn(0, totalSections)
        val numericWorkFinished = completed == totalSections

        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(58.dp),
        )
        Text(
            if (numericWorkFinished) "Números preenchidos" else "Relatório em andamento",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Você pode fechar o aplicativo. Cada alteração fica salva automaticamente.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryLine("Formulário", draft.templateId.displayNamePtBr)
                SummaryLine("Mês", "${MonthNames[draft.identity.month - 1]} de ${draft.identity.year}")
                SummaryLine("Congregação", draft.identity.congregation)
                SummaryLine("Regional", draft.identity.regional.ifBlank { "Não informada" })
                SummaryLine("Pastor dirigente", draft.identity.leadPastor.ifBlank { "Não informado" })
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (numericWorkFinished) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (numericWorkFinished) "Etapa numérica concluída" else "Progresso do preenchimento",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "$completed de $totalSections seções revisadas",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (numericWorkFinished) {
                        "Você pode revisar qualquer seção e preencher as movimentações e anotações."
                    } else {
                        "Os valores podem permanecer em zero quando não houve atividade no mês."
                    },
                    color = if (numericWorkFinished) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
            }
        }

        LargeActionButton(
            text = when {
                numericWorkFinished -> "Revisar números"
                completed == 0 -> "Começar preenchimento"
                else -> "Continuar preenchimento"
            },
            supportingText = "${definition.fieldCount} campos em $totalSections seções",
            icon = Icons.Default.Edit,
            onClick = onStartFilling,
        )
        LargeActionButton(
            text = "Movimentação de membros",
            supportingText = if (draft.movements.isEmpty()) {
                "Nenhuma pessoa adicionada"
            } else {
                "${draft.movements.size} pessoa(s) adicionada(s)"
            },
            icon = Icons.Default.FolderOpen,
            onClick = onMovements,
        )
        LargeActionButton(
            text = "Anotações e sugestões",
            supportingText = if (draft.notes.isBlank()) "Opcional" else "Texto salvo",
            icon = Icons.Default.Article,
            onClick = onNotes,
        )
        LargeActionButton(
            text = "Revisar e concluir",
            supportingText = if (numericWorkFinished) {
                "Confira avisos e conclua o relatório"
            } else {
                "Ainda faltam seções numéricas para revisar"
            },
            icon = Icons.Default.CheckCircle,
            onClick = onReview,
        )
        LargeActionButton(
            text = "Editar identificação",
            icon = Icons.Default.Description,
            onClick = onEditIdentity,
            outlined = true,
        )
        LargeActionButton(
            text = "Voltar ao início",
            icon = Icons.Default.History,
            onClick = onHome,
            outlined = true,
        )
    }
}

@Composable
private fun NumericSectionScreen(
    draft: MonthlyReport?,
    sectionIndex: Int,
    completedSectionCount: Int,
    onValueChange: (String, Int) -> Unit,
    onCustomLabelChange: (String, String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (draft == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(ScreenPadding),
        ) {
            Text("Carregando o rascunho...", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val definition = ReportTemplateCatalog.definition(draft.templateId)
    val safeIndex = sectionIndex.coerceIn(0, definition.sections.lastIndex)
    val section = definition.sections[safeIndex]
    val values = draft.values.associateBy(ReportValue::fieldId)
    val isLastSection = safeIndex == definition.sections.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            section.titlePtBr,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Seção ${safeIndex + 1} de ${definition.sections.size}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Use os botões ou toque no número para digitar. Zero também é uma resposta válida.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${completedSectionCount.coerceIn(0, definition.sections.size)} seções já revisadas",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        section.fields.forEach { field ->
            val fieldId = "${section.id}.${field.id}"
            val value = values[fieldId]
            NumericCounterField(
                fieldId = fieldId,
                field = field,
                reportValue = value,
                onValueChange = onValueChange,
                onCustomLabelChange = onCustomLabelChange,
            )
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MinimumActionHeight),
            ) {
                Text("Voltar")
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MinimumActionHeight),
            ) {
                Text(if (isLastSection) "Concluir números" else "Próxima seção")
            }
        }
    }
}

@Composable
private fun NumericCounterField(
    fieldId: String,
    field: ReportFieldDefinition,
    reportValue: ReportValue?,
    onValueChange: (String, Int) -> Unit,
    onCustomLabelChange: (String, String) -> Unit,
) {
    val persistedValue = reportValue?.value ?: 0
    var valueText by remember(fieldId, persistedValue) {
        mutableStateOf(persistedValue.toString())
    }
    val persistedCustomLabel = reportValue?.customLabel
    var customLabel by remember(fieldId, persistedCustomLabel) {
        mutableStateOf(persistedCustomLabel ?: field.labelPtBr)
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (field.allowsCustomLabel) {
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { value ->
                        customLabel = value.take(80)
                        onCustomLabelChange(fieldId, customLabel)
                    },
                    label = { Text("Nome desta reunião") },
                    supportingText = { Text("Você pode trocar o nome usado no formulário antigo.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Quantidade", style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    field.labelPtBr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        val current = valueText.toIntOrNull() ?: persistedValue
                        val next = (current - 1).coerceAtLeast(0)
                        valueText = next.toString()
                        onValueChange(fieldId, next)
                    },
                    modifier = Modifier.size(MinimumActionHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Diminuir ${field.labelPtBr}",
                    )
                }

                OutlinedTextField(
                    value = valueText,
                    onValueChange = { rawValue ->
                        val digits = rawValue.filter(Char::isDigit).take(6)
                        valueText = digits
                        onValueChange(fieldId, digits.toIntOrNull() ?: 0)
                    },
                    label = { Text("Quantidade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )

                OutlinedButton(
                    onClick = {
                        val current = valueText.toIntOrNull() ?: persistedValue
                        val next = (current + 1).coerceAtMost(999_999)
                        valueText = next.toString()
                        onValueChange(fieldId, next)
                    },
                    modifier = Modifier.size(MinimumActionHeight),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Aumentar ${field.labelPtBr}",
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedReportsScreen(
    reports: List<ReportSummary>,
    onOpenReport: (String) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filteredReports = remember(reports, query) {
        ReportHistory.filter(reports = reports, query = query)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        Text(
            "Relatórios prontos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Pesquise pelo mês, ano, congregação ou nome do formulário.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(80) },
            label = { Text("Pesquisar relatórios") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = if (query.isBlank()) {
                "${reports.size} relatório(s) salvo(s)"
            } else {
                "${filteredReports.size} resultado(s) encontrado(s)"
            },
            style = MaterialTheme.typography.labelLarge,
        )

        if (reports.isEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Nenhum relatório foi concluído ainda.",
                    modifier = Modifier.padding(18.dp),
                )
            }
        } else if (filteredReports.isEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Nenhum relatório corresponde à pesquisa. Apague o texto e tente novamente.",
                    modifier = Modifier.padding(18.dp),
                )
            }
        } else {
            filteredReports.forEach { report ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "${MonthNames[report.month - 1]} de ${report.year}",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(report.congregation)
                        Text(
                            report.templateId.displayNamePtBr,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { onOpenReport(report.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = MinimumActionHeight),
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Abrir relatório")
                        }
                    }
                }
            }
        }
        LargeActionButton(
            text = "Voltar ao início",
            icon = Icons.Default.History,
            onClick = onHome,
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LargeActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    outlined: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.labelLarge)
                if (supportingText != null) {
                    Text(
                        supportingText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Icon(Icons.Default.NavigateNext, contentDescription = null)
        }
    }

    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp),
            contentPadding = PaddingValues(horizontal = 18.dp),
        ) { content() }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp),
            contentPadding = PaddingValues(horizontal = 18.dp),
            colors = ButtonDefaults.buttonColors(),
        ) { content() }
    }
}
