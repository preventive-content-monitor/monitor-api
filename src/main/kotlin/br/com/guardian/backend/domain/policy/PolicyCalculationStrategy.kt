package br.com.guardian.backend.domain.policy

interface PolicyCalculationStrategy {
    fun calculatePolicy(age: Int): PolicyConfiguration
}

data class PolicyConfiguration(
    val mode: PolicyMode,
    val riskThreshold: Int
)
