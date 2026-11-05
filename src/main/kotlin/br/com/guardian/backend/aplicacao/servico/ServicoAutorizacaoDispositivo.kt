package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.dominio.excecao.DispositivoNaoEncontradoExcecao
import br.com.guardian.backend.dominio.modelo.Dispositivo
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ServicoAutorizacaoDispositivo(
    private val dispositivoRepositorio: DispositivoRepositorio
) {
    fun verificar(dispositivoId: UUID, principal: Jwt, permiteResponsavel: Boolean = false): Dispositivo {
        val dispositivo = dispositivoRepositorio.findById(dispositivoId)
            .orElseThrow { DispositivoNaoEncontradoExcecao() }
        val escopo = principal.getClaimAsString("scope")
        val autorizado = when (escopo) {
            TipoCredencial.DISPOSITIVO.escopo -> principal.subject == dispositivoId.toString()
            TipoCredencial.RESPONSAVEL.escopo, TipoCredencial.VINCULACAO.escopo ->
                permiteResponsavel && principal.subject == dispositivo.dependente.usuarioGuardian.id.toString()
            else -> false
        }
        if (!autorizado) throw DispositivoNaoEncontradoExcecao()
        return dispositivo
    }
}
