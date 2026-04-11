package br.com.guardian.backend.domain.policy

interface EstrategiaCalculoPolitica {
    fun calcularPolitica(idade: Int): ConfiguracaoPolitica
}

data class ConfiguracaoPolitica(
    val modo: ModoPolitica,
    val limiteRisco: Int
)
