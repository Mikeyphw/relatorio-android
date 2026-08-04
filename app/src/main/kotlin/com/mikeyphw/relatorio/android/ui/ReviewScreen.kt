package com.mikeyphw.relatorio.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportReviewIssue
import com.mikeyphw.relatorio.model.ReportReviewSeverity
import com.mikeyphw.relatorio.model.ReportTemplateCatalog
import com.mikeyphw.relatorio.model.ReportValue

private val ReviewPadding = 20.dp
private val ReviewGap = 18.dp
private val ReviewActionHeight = 60.dp

@Composable
internal fun ReviewScreen(
    draft: MonthlyReport?,
    completedSectionCount: Int,
    issues: List<ReportReviewIssue>,
    canComplete: Boolean,
    saving: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmation by remember { mutableStateOf(false) }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Concluir este relatório?") },
            text = {
                Text(
                    "Depois de concluir, ele sairá dos rascunhos e aparecerá em Relatórios prontos.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmation = false
                        onComplete()
                    },
                    enabled = !saving,
                ) {
                    Text(if (saving) "Concluindo..." else "Sim, concluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Ainda não")
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ReviewPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(ReviewGap),
    ) {
        Text(
            "Revisão final",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Confira os avisos antes de marcar o relatório como concluído.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (draft == null) {
            Text("Carregando o rascunho...")
            return@Column
        }

        IdentityReviewCard(draft)
        NumericReviewCard(draft, completedSectionCount)
        ContentReviewCard(draft)

        val blocking = issues.filter { it.severity == ReportReviewSeverity.Blocking }
        val warnings = issues.filter { it.severity == ReportReviewSeverity.Warning }

        when {
            issues.isEmpty() -> ReviewReadyCard()
            else -> {
                if (blocking.isNotEmpty()) {
                    ReviewIssueGroup(
                        title = "Corrija antes de concluir",
                        issues = blocking,
                        severity = ReportReviewSeverity.Blocking,
                    )
                }
                if (warnings.isNotEmpty()) {
                    ReviewIssueGroup(
                        title = "Avisos para conferir",
                        issues = warnings,
                        severity = ReportReviewSeverity.Warning,
                    )
                }
            }
        }

        Button(
            onClick = { showConfirmation = true },
            enabled = canComplete && !saving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ReviewActionHeight),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(if (saving) "Concluindo..." else "Concluir relatório")
        }

        if (!canComplete) {
            Text(
                "O botão será liberado depois que os itens vermelhos forem corrigidos.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (warnings.isNotEmpty()) {
            Text(
                "Os avisos amarelos não impedem a conclusão. Eles servem para uma última conferência.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun IdentityReviewCard(draft: MonthlyReport) {
    val month = ReviewMonthNames.getOrElse(draft.identity.month - 1) { "Mês inválido" }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Identificação", style = MaterialTheme.typography.titleLarge)
            ReviewLine("Formulário", draft.templateId.displayNamePtBr)
            ReviewLine("Período", "$month de ${draft.identity.year}")
            ReviewLine("Congregação", draft.identity.congregation.ifBlank { "Não informada" })
            ReviewLine("Regional", draft.identity.regional.ifBlank { "Não informada" })
            ReviewLine("Pastor dirigente", draft.identity.leadPastor.ifBlank { "Não informado" })
        }
    }
}

@Composable
private fun NumericReviewCard(
    draft: MonthlyReport,
    completedSectionCount: Int,
) {
    val definition = ReportTemplateCatalog.definition(draft.templateId)
    val values = draft.values.associateBy(ReportValue::fieldId)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FactCheck, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Seções numéricas", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "$completedSectionCount de ${definition.sections.size} seções revisadas",
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            definition.sections.forEach { section ->
                val nonZero = section.fields.count { field ->
                    values["${section.id}.${field.id}"]?.value?.let { it > 0 } == true
                }
                ReviewLine(section.titlePtBr, "$nonZero de ${section.fields.size} campo(s) acima de zero")
            }
            Text(
                "Campos com zero continuam válidos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ContentReviewCard(draft: MonthlyReport) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Informações adicionais", style = MaterialTheme.typography.titleLarge)
            ReviewLine("Movimentações", "${draft.movements.size} pessoa(s)")
            ReviewLine("Anotações", if (draft.notes.isBlank()) "Sem anotações" else "Texto preenchido")
        }
    }
}

@Composable
private fun ReviewReadyCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Tudo pronto",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "Nenhuma inconsistência foi encontrada.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ReviewIssueGroup(
    title: String,
    issues: List<ReportReviewIssue>,
    severity: ReportReviewSeverity,
) {
    val isBlocking = severity == ReportReviewSeverity.Blocking
    val container = if (isBlocking) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = if (isBlocking) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isBlocking) Icons.Default.Error else Icons.Default.Warning,
                    contentDescription = null,
                    tint = content,
                )
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = content)
            }
            issues.forEachIndexed { index, issue ->
                if (index > 0) HorizontalDivider(color = content.copy(alpha = 0.25f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(issue.titlePtBr, fontWeight = FontWeight.Bold, color = content)
                    Text(issue.messagePtBr, color = content)
                }
            }
        }
    }
}

@Composable
private fun ReviewLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

private val ReviewMonthNames = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
)
