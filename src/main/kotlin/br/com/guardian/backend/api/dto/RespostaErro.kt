package br.com.guardian.backend.api.dto

import java.time.Instant

data class RespostaErro(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val erro: String,
    val mensagem: String,
    val caminho: String,
    val detalhes: List<String>? = null
)
