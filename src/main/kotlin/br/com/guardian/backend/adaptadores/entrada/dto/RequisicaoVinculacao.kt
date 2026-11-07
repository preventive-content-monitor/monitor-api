package br.com.guardian.backend.adaptadores.entrada.dto

data class RequisicaoVinculacao(
    val codigo: String,
    val nomeDispositivo: String,
    /** Identificador do computador que sobrevive à reinstalação da extensão (opcional). */
    val computadorId: String? = null
)