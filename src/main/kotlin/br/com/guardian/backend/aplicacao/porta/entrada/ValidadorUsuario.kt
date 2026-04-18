package br.com.guardian.backend.aplicacao.porta.entrada

import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoLogin
import br.com.guardian.backend.dominio.modelo.UsuarioGuardian

interface ValidadorUsuario {
    fun validarRegistro(email: String)
    fun validarLogin(requisicao: RequisicaoLogin): UsuarioGuardian
}
