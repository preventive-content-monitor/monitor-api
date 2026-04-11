package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.RespostaAutenticacao
import br.com.guardian.backend.api.dto.RequisicaoLogin
import br.com.guardian.backend.api.dto.RequisicaoRegistro
import br.com.guardian.backend.domain.user.UsuarioGuardian
import br.com.guardian.backend.domain.user.UsuarioRepositorio
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
