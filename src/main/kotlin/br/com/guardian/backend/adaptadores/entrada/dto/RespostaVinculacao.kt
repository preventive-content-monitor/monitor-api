package br.com.guardian.backend.adaptadores.entrada.dto

import java.time.Instant
import java.util.UUID

data class RespostaVinculacao(
    val id: UUID,
    val nomeDispositivo: String,
    val vinculadoEm: Instant,
    val apelidoDependente: String,
    val tokenDispositivo: String
) {
    override fun toString(): String = "RespostaVinculacao(id=$id, tokenDispositivo=[PROTEGIDO])"
}
