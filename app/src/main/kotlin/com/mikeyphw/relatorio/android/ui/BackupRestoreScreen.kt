package com.mikeyphw.relatorio.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mikeyphw.relatorio.model.BackupRestorePreview
import java.time.LocalDate

private val BackupPadding = 20.dp
private val BackupGap = 18.dp
private val BackupActionHeight = 64.dp

@Composable
internal fun BackupRestoreScreen(
    hasCurrentDraft: Boolean,
    busy: Boolean,
    message: String?,
    messageIsError: Boolean,
    preview: BackupRestorePreview?,
    replaceCurrentDraft: Boolean,
    onExport: (android.net.Uri, Boolean) -> Unit,
    onLoad: (android.net.Uri) -> Unit,
    onReplaceCurrentDraftChange: (Boolean) -> Unit,
    onRestore: () -> Unit,
    onDismissPreview: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmRestore by remember(preview, replaceCurrentDraft) { mutableStateOf(false) }
    val today = LocalDate.now()
    val backupName = "Relatorio_Mensal_AD_backup_${today}.adbackup.xml"
    val bundleName = "Relatorio_Mensal_AD_pacote_${today}.zip"

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml"),
    ) { uri -> if (uri != null) onExport(uri, false) }
    val bundleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> if (uri != null) onExport(uri, true) }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onLoad(uri) }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restaurar este backup?") },
            text = {
                Text(
                    when {
                        preview?.willReplaceCurrentDraft == true ->
                            "O rascunho atual será substituído. As configurações também serão restauradas. Relatórios repetidos serão ignorados."
                        preview?.importableReports?.isEmpty() == true ->
                            "Não há relatórios novos. Somente as configurações e preferências serão restauradas."
                        else ->
                            "Os relatórios novos serão acrescentados e as configurações serão restauradas. Relatórios repetidos serão ignorados."
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmRestore = false
                        onRestore()
                    },
                ) { Text("Restaurar agora") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancelar") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(BackupPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(BackupGap),
    ) {
        Text(
            "Backup e restauração",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Guarde uma cópia antes de trocar de celular, reinstalar o aplicativo ou fazer uma correção importante.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        message?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (messageIsError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        if (messageIsError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(it)
                        TextButton(onClick = onClearMessage) { Text("Fechar mensagem") }
                    }
                }
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Salvar uma cópia", style = MaterialTheme.typography.titleLarge)
                Text(
                    "O backup inclui relatórios prontos, rascunho, movimentações, anotações e preferências de leitura.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { backupLauncher.launch(backupName) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = BackupActionHeight),
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (busy) "Aguarde..." else "Salvar backup dos dados")
                }
                OutlinedButton(
                    onClick = { bundleLauncher.launch(bundleName) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = BackupActionHeight),
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Salvar pacote com PDFs")
                }
                Text(
                    "O pacote ZIP contém o backup restaurável e um PDF de cada relatório concluído.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Restaurar uma cópia", style = MaterialTheme.typography.titleLarge)
                Text(
                    "O aplicativo primeiro confere o arquivo e mostra o que será restaurado. Nada é alterado antes da sua confirmação.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        restoreLauncher.launch(
                            arrayOf("application/xml", "text/xml", "application/zip", "application/octet-stream", "text/*"),
                        )
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = BackupActionHeight),
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Escolher arquivo de backup")
                }
            }
        }

        preview?.let { selectedPreview ->
            RestorePreviewCard(
                preview = selectedPreview,
                hasCurrentDraft = hasCurrentDraft,
                replaceCurrentDraft = replaceCurrentDraft,
                busy = busy,
                onReplaceCurrentDraftChange = onReplaceCurrentDraftChange,
                onConfirm = { confirmRestore = true },
                onCancel = onDismissPreview,
            )
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacidade", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Os dados ficam no aparelho. Um arquivo só sai do aplicativo quando você escolhe salvar ou compartilhar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RestorePreviewCard(
    preview: BackupRestorePreview,
    hasCurrentDraft: Boolean,
    replaceCurrentDraft: Boolean,
    busy: Boolean,
    onReplaceCurrentDraftChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Conferência do backup", style = MaterialTheme.typography.titleLarge)
            Text("${preview.totalReportsInBackup} relatório(s) no arquivo")
            Text("${preview.completedReportsInBackup} concluído(s)")
            Text("${preview.draftReportsInBackup} em andamento")
            Text("${preview.importableReports.size} novo(s) para restaurar")
            if (preview.willRestorePreferences) {
                Text("Configurações de identificação, formulário, leitura e contraste serão restauradas")
            }
            if (preview.skippedDuplicateCount > 0) {
                Text("${preview.skippedDuplicateCount} repetido(s) serão ignorados")
            }

            preview.blockingErrors.forEach { error ->
                Text("• $error", color = MaterialTheme.colorScheme.error)
            }
            preview.warnings.forEach { warning ->
                Text("• $warning", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (hasCurrentDraft && preview.draftReportsInBackup > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = replaceCurrentDraft,
                        onCheckedChange = onReplaceCurrentDraftChange,
                        enabled = !busy,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Substituir o relatório em andamento atual")
                }
            }

            Button(
                onClick = onConfirm,
                enabled = !busy && preview.canRestore,
                modifier = Modifier.fillMaxWidth().heightIn(min = BackupActionHeight),
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (preview.importableReports.isEmpty()) {
                        "Restaurar configurações"
                    } else {
                        "Restaurar ${preview.importableReports.size} relatório(s) e configurações"
                    },
                )
            }
            TextButton(
                onClick = onCancel,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cancelar restauração") }
        }
    }
}
