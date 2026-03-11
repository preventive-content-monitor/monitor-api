package br.com.guardian.backend.api.dto

data class CreateDependentRequest(
    val nickname: String,
    val birthYear: Int
)