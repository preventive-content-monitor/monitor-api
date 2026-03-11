package br.com.guardian.backend.api.dto

data class RegisterRequest(
    val email: String,
    val password: String
)