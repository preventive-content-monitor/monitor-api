package br.com.guardian.backend.auth

import br.com.guardian.backend.domain.user.UsuarioGuardian
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Component
class JwtGeradorToken(
    @Value("\${guardian.security.jwt.secret}")
    private val jwtSegredo: String
) : GeradorToken {

    override fun gerarToken(usuario: UsuarioGuardian): String {
        val chave = SecretKeySpec(jwtSegredo.toByteArray(), SignatureAlgorithm.HS256.jcaName)

        return Jwts.builder()
            .setSubject(usuario.id.toString())
            .claim("role", usuario.papel.name)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
            .signWith(chave)
            .compact()
    }
}
