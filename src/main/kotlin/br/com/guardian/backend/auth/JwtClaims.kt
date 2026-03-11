package br.com.guardian.backend.auth

data class JwtClaims(
    val sub: String,
    val role: String
)