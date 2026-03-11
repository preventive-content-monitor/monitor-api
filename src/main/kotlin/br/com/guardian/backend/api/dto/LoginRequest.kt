package br.com.guardian.backend.api.dto

data class LoginRequest(
    val email: String,
    val password: String
)