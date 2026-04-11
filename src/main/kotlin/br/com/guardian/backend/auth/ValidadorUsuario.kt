package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.RequisicaoLogin
import br.com.guardian.backend.domain.user.UsuarioGuardian

interface ValidadorUsuario {
    fun validarRegistro(email: String)
    fun validarLogin(requisicao: RequisicaoLogin): UsuarioGuardian
}
