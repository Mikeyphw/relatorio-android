package com.mikeyphw.relatorio.model

data class MovementReason(
    val code: String,
    val labelPtBr: String,
)

object MovementReasonCatalog {
    val all = listOf(
        MovementReason("FCO", "Fora de comunhão"),
        MovementReason("RCO", "Recebidos em comunhão"),
        MovementReason("EXC", "Desligados"),
        MovementReason("CME", "Carta de mudança expedida"),
        MovementReason("CMR", "Carta de mudança recebida"),
        MovementReason("BNA", "Batismo nas águas"),
        MovementReason("FAL", "Falecimento"),
    )

    fun byCode(code: String): MovementReason? = all.firstOrNull { it.code == code }
}
