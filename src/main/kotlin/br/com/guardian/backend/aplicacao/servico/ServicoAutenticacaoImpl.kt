package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaAutenticacao
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoLogin
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoRegistro
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoAutenticacao
import br.com.guardian.backend.aplicacao.porta.entrada.ValidadorUsuario
import br.com.guardian.backend.adaptadores.saida.persistencia.UsuarioRepositorio
import br.com.guardian.backend.dominio.modelo.UsuarioGuardian
import br.com.guardian.backend.dominio.porta.GeradorToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class ServicoAutenticacaoImpl(
    private val usuarioRepositorio: UsuarioRepositorio,
    private val codificadorSenha: PasswordEncoder,
    private val validadorUsuario: ValidadorUsuario,
    private val geradorToken: GeradorToken
) : ServicoAutenticacao {

    override fun registrar(requisicao: RequisicaoRegistro) {
        validadorUsuario.validarRegistro(requisicao.email)

        val usuario = UsuarioGuardian(
            email = requisicao.email,
            senhaHash = codificadorSenha.encode(requisicao.senha)
        )

        usuarioRepositorio.save(usuario)
    }

    override fun autenticar(requisicao: RequisicaoLogin): RespostaAutenticacao {
        val usuario = validadorUsuario.validarLogin(requisicao)
        val token = geradorToken.gerarToken(usuario)
        return RespostaAutenticacao(token)
    }
}
