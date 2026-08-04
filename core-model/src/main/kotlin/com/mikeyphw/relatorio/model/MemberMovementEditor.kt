package com.mikeyphw.relatorio.model

import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.UUID

private val BrazilianDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

private val IsoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

data class MemberMovementInput(
    val registrationNumber: String = "",
    val memberName: String = "",
    val eventDateText: String = "",
    val reasonCode: String = MovementReasonCatalog.all.first().code,
    val occurrence: String = "",
    val birthDateText: String = "",
)

object BrazilianDateInput {
    fun normalizeTyping(rawValue: String): String {
        val digits = rawValue.filter(Char::isDigit).take(8)
        return buildString {
            digits.forEachIndexed { index, char ->
                if (index == 2 || index == 4) append('/')
                append(char)
            }
        }
    }

    fun toIsoOrNull(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        return try {
            LocalDate.parse(trimmed, BrazilianDateFormatter).format(IsoDateFormatter)
        } catch (_: DateTimeException) {
            null
        }
    }

    fun fromIso(value: String): String {
        if (value.isBlank()) return ""
        return try {
            LocalDate.parse(value, IsoDateFormatter).format(BrazilianDateFormatter)
        } catch (_: DateTimeException) {
            ""
        }
    }
}

object MemberMovementValidator {
    fun validate(
        input: MemberMovementInput,
        includesBirthDate: Boolean,
    ): List<String> = buildList {
        if (input.memberName.isBlank()) add("Informe o nome da pessoa.")
        if (MovementReasonCatalog.byCode(input.reasonCode) == null) {
            add("Escolha um motivo válido.")
        }
        if (BrazilianDateInput.toIsoOrNull(input.eventDateText) == null) {
            add("Informe a data da ocorrência no formato dia/mês/ano.")
        }
        if (includesBirthDate && BrazilianDateInput.toIsoOrNull(input.birthDateText) == null) {
            add("Informe uma data de nascimento válida ou deixe o campo vazio.")
        }
    }

    fun toMovement(
        input: MemberMovementInput,
        includesBirthDate: Boolean,
        id: String = UUID.randomUUID().toString(),
    ): MemberMovement {
        val messages = validate(input, includesBirthDate)
        require(messages.isEmpty()) { messages.joinToString(separator = " ") }
        val reason = requireNotNull(MovementReasonCatalog.byCode(input.reasonCode))
        return MemberMovement(
            id = id,
            registrationNumber = input.registrationNumber.trim().take(30),
            memberName = input.memberName.trim().take(160),
            eventDateIso = requireNotNull(BrazilianDateInput.toIsoOrNull(input.eventDateText)),
            reasonCode = reason.code,
            occurrence = if (includesBirthDate) {
                input.occurrence.trim().take(200).ifBlank { reason.labelPtBr }
            } else {
                ""
            },
            birthDateIso = if (includesBirthDate) {
                requireNotNull(BrazilianDateInput.toIsoOrNull(input.birthDateText))
            } else {
                ""
            },
        )
    }

    fun fromMovement(movement: MemberMovement): MemberMovementInput = MemberMovementInput(
        registrationNumber = movement.registrationNumber,
        memberName = movement.memberName,
        eventDateText = BrazilianDateInput.fromIso(movement.eventDateIso),
        reasonCode = movement.reasonCode,
        occurrence = movement.occurrence,
        birthDateText = BrazilianDateInput.fromIso(movement.birthDateIso),
    )
}
