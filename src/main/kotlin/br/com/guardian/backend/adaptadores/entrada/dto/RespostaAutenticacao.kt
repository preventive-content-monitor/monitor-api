package br.com.guardian.backend.adaptadores.entrada.dto

data class RespostaAutenticacao(
    val token: String
) {
    override fun toString(): String = "RespostaAutenticacao(token=[PROTEGIDO])"
}