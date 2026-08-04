package com.mikeyphw.relatorio.android.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikeyphw.relatorio.android.pdf.ReportPdfExporter
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.ReportPdfContentFactory
import java.io.File
import kotlinx.coroutines.launch

private val ExportScreenPadding = 20.dp
private val ExportGap = 18.dp
private val ExportActionHeight = 64.dp
private val ExportMonthNames = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
)

@Composable
internal fun CompletedReportDetailScreen(
    report: MonthlyReport?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exporter = remember(context) { ReportPdfExporter(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var busy by remember(report?.id) { mutableStateOf(false) }
    var message by remember(report?.id) { mutableStateOf<String?>(null) }
    var messageIsError by remember(report?.id) { mutableStateOf(false) }
    var pendingSaveFile by remember(report?.id) { mutableStateOf<File?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destination ->
        val source = pendingSaveFile
        pendingSaveFile = null
        if (destination == null || source == null) {
            if (destination == null) {
                message = "O salvamento foi cancelado."
                messageIsError = false
            }
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            busy = true
            runCatching { exporter.copyTo(source, destination) }
                .onSuccess {
                    message = "PDF salvo no local escolhido."
                    messageIsError = false
                }
                .onFailure { error ->
                    message = error.userMessage("Não foi possível salvar o PDF.")
                    messageIsError = true
                }
            busy = false
        }
    }

    fun generateAnd(action: (File) -> Unit) {
        val selectedReport = report ?: return
        scope.launch {
            busy = true
            message = null
            runCatching { exporter.generate(selectedReport) }
                .onSuccess(action)
                .onFailure { error ->
                    message = error.userMessage("Não foi possível gerar o PDF.")
                    messageIsError = true
                }
            busy = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ExportScreenPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(ExportGap),
    ) {
        Text(
            "PDF do relatório",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Visualize, salve ou compartilhe o documento pronto.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (report == null) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text("Carregando o relatório...", modifier = Modifier.padding(18.dp))
            }
            return@Column
        }

        ReportExportSummary(report)

        Button(
            onClick = {
                generateAnd { file ->
                    runCatching { openPdf(context, exporter, file) }
                        .onSuccess {
                            message = "PDF aberto para visualização."
                            messageIsError = false
                        }
                        .onFailure { error ->
                            message = error.userMessage("Nenhum aplicativo conseguiu abrir o PDF.")
                            messageIsError = true
                        }
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(ExportActionHeight),
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(if (busy) "Preparando PDF..." else "Visualizar PDF")
        }

        OutlinedButton(
            onClick = {
                generateAnd { file ->
                    pendingSaveFile = file
                    saveLauncher.launch(file.name)
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(ExportActionHeight),
        ) {
            Icon(Icons.Default.SaveAlt, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Salvar uma cópia")
        }

        OutlinedButton(
            onClick = {
                generateAnd { file ->
                    runCatching { sharePdf(context, exporter, file) }
                        .onSuccess {
                            message = "Escolha onde deseja compartilhar o PDF."
                            messageIsError = false
                        }
                        .onFailure { error ->
                            message = error.userMessage("Não foi possível compartilhar o PDF.")
                            messageIsError = true
                        }
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(ExportActionHeight),
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Compartilhar PDF")
        }

        if (message != null) {
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
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (messageIsError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(message.orEmpty(), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text(
            "O arquivo é gerado no aparelho. Nenhuma informação é enviada para a internet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReportExportSummary(report: MonthlyReport) {
    val month = ExportMonthNames.getOrElse(report.identity.month - 1) { "Mês ${report.identity.month}" }
    val fileName = ReportPdfContentFactory.suggestedFileName(report)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(report.identity.congregation, style = MaterialTheme.typography.titleLarge)
                    Text("$month de ${report.identity.year}")
                }
            }
            Text(
                report.templateId.displayNamePtBr,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                fileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun openPdf(context: Context, exporter: ReportPdfExporter, file: File) {
    val uri = exporter.contentUri(file)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    try {
        context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        throw IllegalStateException("Instale um leitor de PDF para visualizar o arquivo.", error)
    }
}

private fun sharePdf(context: Context, exporter: ReportPdfExporter, file: File) {
    val uri = exporter.contentUri(file)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar relatório"))
}

private fun Throwable.userMessage(fallback: String): String =
    message?.takeIf(String::isNotBlank) ?: fallback
