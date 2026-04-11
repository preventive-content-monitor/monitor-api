package br.com.guardian.backend.domain.policy

import org.springframework.stereotype.Component

@Component
class AgeBasedPolicyCalculationStrategy : PolicyCalculationStrategy {

    override fun calculatePolicy(age: Int): PolicyConfiguration {
        return when {
            age <= 10 -> PolicyConfiguration(PolicyMode.BLOCK, 30)
            age <= 13 -> PolicyConfiguration(PolicyMode.WARN, 50)
            else -> PolicyConfiguration(PolicyMode.EDUCATE, 70)
        }
    }
}
