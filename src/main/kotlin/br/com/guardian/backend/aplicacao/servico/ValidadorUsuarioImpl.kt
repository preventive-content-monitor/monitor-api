package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoLogin
import br.com.guardian.backend.aplicacao.porta.entrada.ValidadorUsuario
import br.com.guardian.backend.adaptadores.saida.persistencia.UsuarioRepositorio
import br.com.guardian.backend.dominio.modelo.UsuarioGuardian
import br.com.guardian.backend.dominio.excecao.EmailJaExisteExcecao
import br.com.guardian.backend.dominio.excecao.CredenciaisInvalidasExcecao
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
