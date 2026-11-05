package br.com.guardian.backend.adaptadores.entrada.dto

data class RequisicaoRegistro(
    val email: String,
    val senha: String
) {
    override fun toString(): String = "RequisicaoRegistro(email=$email, senha=[PROTEGIDA])"
}