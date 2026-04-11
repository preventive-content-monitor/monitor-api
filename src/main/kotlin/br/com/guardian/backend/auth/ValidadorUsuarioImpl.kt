package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.RequisicaoLogin
import br.com.guardian.backend.domain.user.UsuarioGuardian
import br.com.guardian.backend.domain.user.UsuarioRepositorio
import br.com.guardian.backend.exception.EmailJaExisteExcecao
import br.com.guardian.backend.exception.CredenciaisInvalidasExcecao
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class ValidadorUsuarioImpl(
    private val usuarioRepositorio: UsuarioRepositorio,
    private val codificadorSenha: PasswordEncoder
) : ValidadorUsuario {

    override fun validarRegistro(email: String) {
        if (usuarioRepositorio.findByEmail(email) != null) {
            throw EmailJaExisteExcecao()
        }
    }

    override fun validarLogin(requisicao: RequisicaoLogin): UsuarioGuardian {
        val usuario = usuarioRepositorio.findByEmail(requisicao.email)
            ?: throw CredenciaisInvalidasExcecao()

        if (!codificadorSenha.matches(requisicao.senha, usuario.senhaHash)) {
            throw CredenciaisInvalidasExcecao()
        }

        return usuario
    }
}
