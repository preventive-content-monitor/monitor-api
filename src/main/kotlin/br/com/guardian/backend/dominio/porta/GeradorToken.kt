package br.com.guardian.backend.dominio.porta

import br.com.guardian.backend.dominio.modelo.UsuarioGuardian
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import java.util.UUID

interface GeradorToken {
    fun gerarToken(usuario: UsuarioGuardian, tipo: TipoCredencial = TipoCredencial.RESPONSAVEL): String
    fun gerarTokenDispositivo(dispositivoId: UUID): String
    fun revogarToken(sessaoId: UUID)
}
