package br.com.guardian.backend.domain.policy

import br.com.guardian.backend.domain.dependent.Dependente
import org.springframework.stereotype.Component
import java.time.Year

@Component
class CalculadorIdade {

    fun calcularIdade(dependente: Dependente): Int {
        return Year.now().value - dependente.anoNascimento
    }
}
