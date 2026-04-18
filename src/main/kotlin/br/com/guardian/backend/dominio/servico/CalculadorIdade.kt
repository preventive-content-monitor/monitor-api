package br.com.guardian.backend.dominio.servico

import br.com.guardian.backend.dominio.modelo.Dependente
import org.springframework.stereotype.Component
import java.time.Year

@Component
class CalculadorIdade {

    fun calcularIdade(dependente: Dependente): Int {
        return Year.now().value - dependente.anoNascimento
    }
}
