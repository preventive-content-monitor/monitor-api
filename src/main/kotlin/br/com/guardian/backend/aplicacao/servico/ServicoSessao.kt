package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaAutenticacao
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoLogin
import br.com.guardian.backend.adaptadores.saida.persistencia.SessaoCredencialRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.UsuarioRepositorio
import br.com.guardian.backend.aplicacao.porta.entrada.ValidadorUsuario
import br.com.guardian.backend.dominio.excecao.UsuarioNaoEncontradoExcecao
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import br.com.guardian.backend.dominio.porta.GeradorToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ServicoSessao(
    private val usuarioRepositorio: UsuarioRepositorio,
    private val geradorToken: GeradorToken,
    private val sessoes: SessaoCredencialRepositorio,
    private val validadorUsuario: ValidadorUsuario
) {
    @Transactional
    fun iniciarConfiguracao(principal: Jwt): RespostaAutenticacao {
        val usuario = usuarioRepositorio.findById(UUID.fromString(principal.subject))
            .orElseThrow { UsuarioNaoEncontradoExcecao() }
        val token = geradorToken.gerarToken(usuario, TipoCredencial.CONFIGURACAO)
        revogarSessoesResponsavel(usuario.id)
        return RespostaAutenticacao(token)
    }

    @Transactional
    fun autenticarConfiguracao(requisicao: RequisicaoLogin): RespostaAutenticacao {
        val usuario = validadorUsuario.validarLogin(requisicao)
        revogarSessoesResponsavel(usuario.id)
        return RespostaAutenticacao(geradorToken.gerarToken(usuario, TipoCredencial.CONFIGURACAO))
    }

    @Transactional
    fun autorizarVinculacao(principal: Jwt): RespostaAutenticacao {
        val usuario = usuarioRepositorio.findById(UUID.fromString(principal.subject))
            .orElseThrow { UsuarioNaoEncontradoExcecao() }
        return RespostaAutenticacao(geradorToken.gerarToken(usuario, TipoCredencial.VINCULACAO))
    }

    @Transactional
    fun encerrar(principal: Jwt) {
        geradorToken.revogarToken(UUID.fromString(principal.id))
    }

    private fun revogarSessoesResponsavel(usuarioId: UUID) {
        sessoes.revogarSessoes(usuarioId, TipoCredencial.RESPONSAVEL, Instant.now())
    }
}
