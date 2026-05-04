package br.com.guardian.backend.dominio.servico

import br.com.guardian.backend.dominio.modelo.Dependente
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period
import java.time.Year

@Component
class CalculadorIdade {

    fun calcularIdade(dependente: Dependente): Int {
        val dataNascimento = dependente.dataNascimento
        if (dataNascimento != null) {
            return Period.between(dataNascimento, LocalDate.now()).years
        }

        return Year.now().value - dependente.anoNascimento
    }
}
