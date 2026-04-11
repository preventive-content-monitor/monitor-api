package br.com.guardian.backend.domain.policy

import org.springframework.stereotype.Component

@Component
class EstrategiaCalculoPoliticaPorIdade : EstrategiaCalculoPolitica {

    override fun calcularPolitica(idade: Int): ConfiguracaoPolitica {
        return when {
            idade <= 10 -> ConfiguracaoPolitica(ModoPolitica.BLOCK, 30)
            idade <= 13 -> ConfiguracaoPolitica(ModoPolitica.WARN, 50)
            else -> ConfiguracaoPolitica(ModoPolitica.EDUCATE, 70)
        }
    }
}
