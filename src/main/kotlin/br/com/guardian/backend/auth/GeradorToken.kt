package br.com.guardian.backend.auth

import br.com.guardian.backend.domain.user.UsuarioGuardian

interface GeradorToken {
    fun gerarToken(usuario: UsuarioGuardian): String
}
