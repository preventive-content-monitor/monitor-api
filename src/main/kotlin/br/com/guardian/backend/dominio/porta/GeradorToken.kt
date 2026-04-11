package br.com.guardian.backend.dominio.porta

import br.com.guardian.backend.dominio.modelo.UsuarioGuardian

interface GeradorToken {
    fun gerarToken(usuario: UsuarioGuardian): String
}
