package com.mikeyphw.relatorio.model

import java.util.UUID

enum class ReportTemplateId(
    val stableId: String,
    val displayNamePtBr: String,
    val descriptionPtBr: String,
    val isDefault: Boolean,
) {
    AdTimoteo2026(
        stableId = "AD_TIMOTEO_2026_V1",
        displayNamePtBr = "Relatório AD Timóteo",
        descriptionPtBr = "Formulário atual e padrão",
        isDefault = true,
    ),
    Complete(
        stableId = "RELATORIO_COMPLETO_V1",
        displayNamePtBr = "Relatório Completo",
        descriptionPtBr = "Formulário anterior, com mais detalhes",
        isDefault = false,
    );

    companion object {
        fun fromStableId(value: String): ReportTemplateId =
            entries.firstOrNull { it.stableId == value } ?: AdTimoteo2026
    }
}

enum class ReportStatus(val storageValue: String) {
    Draft("DRAFT"),
    Completed("COMPLETED");

    companion object {
        fun fromStorageValue(value: String): ReportStatus =
            entries.firstOrNull { it.storageValue == value } ?: Draft
    }
}

data class ReportIdentity(
    val month: Int,
    val year: Int,
    val regional: String = "",
    val congregation: String = "",
    val leadPastor: String = "",
    val ministry: String = "Ministério AD Timóteo MG",
    val presidentPastor: String = "Carmo Matias Gonçalves",
)

data class ReportFieldDefinition(
    val id: String,
    val labelPtBr: String,
    val allowsCustomLabel: Boolean = false,
)

data class ReportSectionDefinition(
    val id: String,
    val titlePtBr: String,
    val fields: List<ReportFieldDefinition>,
)

data class ReportTemplateDefinition(
    val id: ReportTemplateId,
    val sections: List<ReportSectionDefinition>,
    val movementIncludesBirthDate: Boolean,
) {
    val fieldCount: Int = sections.sumOf { it.fields.size }
    val fieldIds: Set<String> = sections
        .flatMap { section -> section.fields.map { field -> "${section.id}.${field.id}" } }
        .toSet()
}

data class ReportValue(
    val fieldId: String,
    val value: Int,
    val customLabel: String? = null,
)

data class MemberMovement(
    val id: String = UUID.randomUUID().toString(),
    val registrationNumber: String = "",
    val memberName: String,
    val eventDateIso: String = "",
    val reasonCode: String,
    val occurrence: String = "",
    val birthDateIso: String = "",
)

data class MonthlyReport(
    val id: String,
    val templateId: ReportTemplateId,
    val status: ReportStatus,
    val identity: ReportIdentity,
    val values: List<ReportValue>,
    val movements: List<MemberMovement>,
    val notes: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class ReportSummary(
    val id: String,
    val templateId: ReportTemplateId,
    val status: ReportStatus,
    val month: Int,
    val year: Int,
    val congregation: String,
    val updatedAtEpochMillis: Long,
)
