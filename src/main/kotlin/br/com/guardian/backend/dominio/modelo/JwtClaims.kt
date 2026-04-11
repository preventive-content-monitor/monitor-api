package br.com.guardian.backend.dominio.modelo

data class JwtClaims(
    val sub: String,
    val role: String
)