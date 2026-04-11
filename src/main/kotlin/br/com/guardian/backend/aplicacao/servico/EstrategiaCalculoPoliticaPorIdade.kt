package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.dominio.modelo.ModoPolitica
import br.com.guardian.backend.dominio.servico.ConfiguracaoPolitica
import br.com.guardian.backend.dominio.servico.EstrategiaCalculoPolitica
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
