package com.mikeyphw.relatorio.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mikeyphw.relatorio.android.HeaderDefaultsForm
import com.mikeyphw.relatorio.android.data.TextSizePreference

@Composable
internal fun AccessibilitySettingsScreen(
    textSizePreference: TextSizePreference,
    highContrast: Boolean,
    headerDefaults: HeaderDefaultsForm,
    savingHeaderDefaults: Boolean,
    settingsMessage: String?,
    settingsMessageIsError: Boolean,
    onTextSizeChange: (TextSizePreference) -> Unit,
    onHighContrastChange: (Boolean) -> Unit,
    pdfOfficialFormLayout: Boolean,
    onPdfOfficialFormLayoutChange: (Boolean) -> Unit,
    onSaveHeaderDefaults: (HeaderDefaultsForm) -> Unit,
    onClearSettingsMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var regional by rememberSaveable(headerDefaults.regional) { mutableStateOf(headerDefaults.regional) }
    var congregation by rememberSaveable(headerDefaults.congregation) { mutableStateOf(headerDefaults.congregation) }
    var leadPastor by rememberSaveable(headerDefaults.leadPastor) { mutableStateOf(headerDefaults.leadPastor) }
    var ministry by rememberSaveable(headerDefaults.ministry) { mutableStateOf(headerDefaults.ministry) }
    var presidentPastor by rememberSaveable(headerDefaults.presidentPastor) {
        mutableStateOf(headerDefaults.presidentPastor)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Defina os dados que aparecem ao iniciar um relatório e ajuste a leitura. O mês sempre começa como o mês anterior.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Text(
                        text = "Valores padrão do cabeçalho",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Text(
                    text = "Esses dados são apenas o ponto de partida. Ao criar o relatório, o usuário pode alterar qualquer campo. Alterar um relatório não muda estes padrões.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HeaderDefaultTextField("Regional", regional) { regional = it.take(120) }
                HeaderDefaultTextField("Congregação", congregation) { congregation = it.take(160) }
                HeaderDefaultTextField("Pastor dirigente", leadPastor) { leadPastor = it.take(160) }
                HeaderDefaultTextField("Ministério", ministry) { ministry = it.take(160) }
                HeaderDefaultTextField("Pastor presidente", presidentPastor) { presidentPastor = it.take(160) }
                Button(
                    enabled = !savingHeaderDefaults,
                    onClick = {
                        onSaveHeaderDefaults(
                            HeaderDefaultsForm(
                                regional = regional,
                                congregation = congregation,
                                leadPastor = leadPastor,
                                ministry = ministry,
                                presidentPastor = presidentPastor,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (savingHeaderDefaults) "Salvando..." else "Salvar valores padrão")
                }
                if (settingsMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (settingsMessageIsError) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = settingsMessage,
                                color = if (settingsMessageIsError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                            )
                            TextButton(onClick = onClearSettingsMessage) { Text("Fechar aviso") }
                        }
                    }
                }
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatSize, contentDescription = null)
                    Text(
                        text = "Tamanho das letras",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                TextSizeChoice(
                    title = "Grande",
                    supportingText = "Tamanho recomendado",
                    selected = textSizePreference == TextSizePreference.Large,
                    onClick = { onTextSizeChange(TextSizePreference.Large) },
                )
                TextSizeChoice(
                    title = "Muito grande",
                    supportingText = "Para ler com mais conforto",
                    selected = textSizePreference == TextSizePreference.ExtraLarge,
                    onClick = { onTextSizeChange(TextSizePreference.ExtraLarge) },
                )
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = highContrast,
                        role = Role.Switch,
                        onClick = { onHighContrastChange(!highContrast) },
                    )
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Default.Contrast, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Contraste reforçado", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Deixa textos, bordas e botões mais destacados.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = highContrast,
                    onCheckedChange = null,
                )
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = pdfOfficialFormLayout,
                        role = Role.Switch,
                        onClick = { onPdfOfficialFormLayoutChange(!pdfOfficialFormLayout) },
                    )
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Layout oficial do formulário", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Gera o PDF padrão AD Timóteo no formato do formulário impresso.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = pdfOfficialFormLayout,
                    onCheckedChange = null,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoStories, contentDescription = null)
                    Text(
                        "Como preencher",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                HelpStep("1", "Toque em Preencher novo relatório.")
                HelpStep("2", "Escolha começar em branco ou copiar o relatório anterior.")
                HelpStep("3", "Confira o mês e os dados da congregação.")
                HelpStep("4", "Use os botões de menos e mais para informar as quantidades.")
                HelpStep("5", "Revise e conclua o relatório, depois abra, salve ou compartilhe o PDF.")
            }
        }

        Text(
            text = "O aplicativo também respeita o tamanho de fonte definido nas configurações do Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeaderDefaultTextField(
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
private fun TextSizeChoice(
    title: String,
    supportingText: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(supportingText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HelpStep(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Passo $number",
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
