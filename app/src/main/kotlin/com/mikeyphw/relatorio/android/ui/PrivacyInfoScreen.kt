package com.mikeyphw.relatorio.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun PrivacyInfoScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Icon(
            Icons.Default.Article,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Privacidade e informações",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Versão 1.0.0",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        InformationCard(
            title = "Seus dados ficam no aparelho",
            body = "O aplicativo funciona sem conta e sem internet. Relatórios, nomes, movimentações e anotações são armazenados somente no celular.",
        )
        InformationCard(
            title = "Você escolhe quando um arquivo sai",
            body = "PDFs e backups só são enviados a outro aplicativo ou local quando você toca em salvar, imprimir ou compartilhar.",
        )
        InformationCard(
            title = "Troca de celular",
            body = "Antes de trocar, salve um backup. No aparelho novo, abra Backup e restauração, escolha o arquivo e confira a prévia.",
        )

        Text(
            "Guia rápido",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        listOf(
            "1. Toque em Preencher novo relatório.",
            "2. Confira mês, congregação e dirigente.",
            "3. Preencha as quantidades e movimentações.",
            "4. Revise os avisos e conclua.",
            "5. Abra o PDF para salvar, imprimir ou compartilhar.",
        ).forEach { step ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Text(step)
                }
            }
        }
    }
}

@Composable
private fun InformationCard(title: String, body: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
