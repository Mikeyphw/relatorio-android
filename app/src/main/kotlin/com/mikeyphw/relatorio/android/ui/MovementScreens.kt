package com.mikeyphw.relatorio.android.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mikeyphw.relatorio.model.BrazilianDateInput
import com.mikeyphw.relatorio.model.MemberMovement
import com.mikeyphw.relatorio.model.MemberMovementInput
import com.mikeyphw.relatorio.model.MonthlyReport
import com.mikeyphw.relatorio.model.MovementReasonCatalog

private val MovementScreenPadding = 20.dp
private val MovementSectionGap = 18.dp
private val MovementActionHeight = 60.dp

@Composable
internal fun MovementsScreen(
    draft: MonthlyReport?,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<MemberMovement?>(null) }

    pendingDelete?.let { movement ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir movimentação?") },
            text = { Text("${movement.memberName} será removido somente deste relatório.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(movement.id)
                        pendingDelete = null
                    },
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MovementScreenPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(MovementSectionGap),
    ) {
        Text(
            "Movimentação de membros",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Adicione somente quem teve uma ocorrência neste mês. O código é escolhido pelo motivo.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MovementActionHeight),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Adicionar pessoa")
        }

        if (draft == null) {
            Text("Carregando o rascunho...")
            return@Column
        }

        if (draft.movements.isEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Text("Nenhuma movimentação adicionada", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tudo bem deixar esta parte vazia quando não houve movimentação.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            draft.movements.forEach { movement ->
                MovementCard(
                    movement = movement,
                    showBirthDate = draft.templateId.isDefault,
                    onEdit = { onEdit(movement.id) },
                    onDelete = { pendingDelete = movement },
                )
            }
        }
    }
}

@Composable
private fun MovementCard(
    movement: MemberMovement,
    showBirthDate: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val reason = MovementReasonCatalog.byCode(movement.reasonCode)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                movement.memberName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            MovementDetail("Registro", movement.registrationNumber.ifBlank { "Não informado" })
            MovementDetail("Data", BrazilianDateInput.fromIso(movement.eventDateIso).ifBlank { "Não informada" })
            MovementDetail(
                "Motivo",
                reason?.let { "${it.labelPtBr} (${it.code})" } ?: movement.reasonCode,
            )
            if (showBirthDate) {
                MovementDetail("Ocorrência", movement.occurrence.ifBlank { "Não informada" })
                MovementDetail(
                    "Nascimento",
                    BrazilianDateInput.fromIso(movement.birthDateIso).ifBlank { "Não informado" },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Editar")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Excluir")
                }
            }
        }
    }
}

@Composable
private fun MovementDetail(label: String, value: String) {
    Text("$label: $value", style = MaterialTheme.typography.bodyLarge)
}

@Composable
internal fun MovementEditorScreen(
    form: MemberMovementInput,
    includesBirthDate: Boolean,
    isEditing: Boolean,
    validationMessages: List<String>,
    saving: Boolean,
    onFormChange: ((MemberMovementInput) -> MemberMovementInput) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MovementScreenPadding)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(MovementSectionGap),
    ) {
        Text(
            if (isEditing) "Editar pessoa" else "Adicionar pessoa",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Campos com asterisco são necessários. As datas usam dia/mês/ano.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (validationMessages.isNotEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Confira estes dados", fontWeight = FontWeight.Bold)
                    validationMessages.forEach { Text("• $it") }
                }
            }
        }

        OutlinedTextField(
            value = form.registrationNumber,
            onValueChange = { value ->
                onFormChange { it.copy(registrationNumber = value.take(30)) }
            },
            label = { Text("Nº de registro") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.memberName,
            onValueChange = { value ->
                onFormChange { it.copy(memberName = value.take(160)) }
            },
            label = { Text("Nome da pessoa *") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.eventDateText,
            onValueChange = { value ->
                onFormChange { it.copy(eventDateText = BrazilianDateInput.normalizeTyping(value)) }
            },
            label = { Text("Data da ocorrência") },
            placeholder = { Text("DD/MM/AAAA") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        ReasonSelector(
            selectedCode = form.reasonCode,
            onSelected = { code ->
                val reason = MovementReasonCatalog.byCode(code)
                onFormChange { current ->
                    current.copy(
                        reasonCode = code,
                        occurrence = if (
                            includesBirthDate &&
                            (current.occurrence.isBlank() || MovementReasonCatalog.all.any { it.labelPtBr == current.occurrence })
                        ) {
                            reason?.labelPtBr.orEmpty()
                        } else {
                            current.occurrence
                        },
                    )
                }
            },
        )

        if (includesBirthDate) {
            OutlinedTextField(
                value = form.occurrence,
                onValueChange = { value ->
                    onFormChange { it.copy(occurrence = value.take(200)) }
                },
                label = { Text("Ocorrência") },
                supportingText = { Text("O motivo é usado automaticamente quando este campo fica vazio.") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.birthDateText,
                onValueChange = { value ->
                    onFormChange { it.copy(birthDateText = BrazilianDateInput.normalizeTyping(value)) }
                },
                label = { Text("Data de nascimento") },
                placeholder = { Text("DD/MM/AAAA") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MovementActionHeight),
        ) {
            Text(if (saving) "Salvando..." else "Salvar pessoa")
        }
    }
}

@Composable
private fun ReasonSelector(
    selectedCode: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = MovementReasonCatalog.byCode(selectedCode) ?: MovementReasonCatalog.all.first()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Motivo *", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MovementActionHeight),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            Text(
                "${selected.labelPtBr} (${selected.code})",
                modifier = Modifier.weight(1f),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MovementReasonCatalog.all.forEach { reason ->
                DropdownMenuItem(
                    text = { Text("${reason.labelPtBr} (${reason.code})") },
                    onClick = {
                        onSelected(reason.code)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun NotesScreen(
    notes: String,
    saving: Boolean,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MovementScreenPadding)
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(MovementSectionGap),
    ) {
        Text(
            "Anotações e sugestões",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Esta parte é opcional. O texto é salvo automaticamente.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Anotações") },
            placeholder = { Text("Digite aqui...") },
            supportingText = {
                Text(if (saving) "Salvando..." else "Salvo automaticamente · ${notes.length}/4000")
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 10,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
