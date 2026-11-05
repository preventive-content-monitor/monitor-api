package br.com.guardian.backend.adaptadores.entrada.dto

data class RequisicaoLogin(
    val email: String,
    val senha: String
) {
    override fun toString(): String = "RequisicaoLogin(email=$email, senha=[PROTEGIDA])"
}