package br.com.guardian.backend.domain.policy

import br.com.guardian.backend.domain.dependent.Dependent
import org.springframework.stereotype.Component
import java.time.Year

@Component
class AgeCalculator {

    fun calculateAge(dependent: Dependent): Int {
        return Year.now().value - dependent.birthYear
    }
}
