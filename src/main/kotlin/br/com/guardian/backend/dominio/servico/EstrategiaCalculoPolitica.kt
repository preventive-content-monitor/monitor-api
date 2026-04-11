package br.com.guardian.backend.dominio.servico

import br.com.guardian.backend.dominio.modelo.ModoPolitica

interface EstrategiaCalculoPolitica {
    fun calcularPolitica(idade: Int): ConfiguracaoPolitica
}

data class ConfiguracaoPolitica(
    val modo: ModoPolitica,
    val limiteRisco: Int
)
