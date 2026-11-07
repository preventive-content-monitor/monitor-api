package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaAutenticacao
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoLogin
import br.com.guardian.backend.adaptadores.saida.persistencia.SessaoCredencialRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.UsuarioRepositorio
import br.com.guardian.backend.aplicacao.porta.entrada.ValidadorUsuario
import br.com.guardian.backend.dominio.excecao.NaoAutorizadoExcecao
import br.com.guardian.backend.dominio.excecao.UsuarioNaoEncontradoExcecao
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import br.com.guardian.backend.dominio.porta.GeradorToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
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

    /**
     * Logo após o login, o responsável ainda está diante do computador: permite desfazer a
     * escolha "computador do menor" sem pedir a senha de novo. Depois da janela, exige login.
     */
    @Transactional
    fun retomarResponsavel(principal: Jwt): RespostaAutenticacao {
        val emitido = principal.issuedAt ?: throw NaoAutorizadoExcecao("Entre novamente com e-mail e senha.")
        if (Duration.between(emitido, Instant.now()) > JANELA_RETOMADA) {
            throw NaoAutorizadoExcecao("Por segurança, entre novamente com e-mail e senha.")
        }
        val usuario = usuarioRepositorio.findById(UUID.fromString(principal.subject))
            .orElseThrow { UsuarioNaoEncontradoExcecao() }
        geradorToken.revogarToken(UUID.fromString(principal.id))
        return RespostaAutenticacao(geradorToken.gerarToken(usuario))
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

    private companion object {
        val JANELA_RETOMADA: Duration = Duration.ofMinutes(15)
    }
}
