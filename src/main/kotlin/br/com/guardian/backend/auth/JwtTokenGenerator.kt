package br.com.guardian.backend.auth

import br.com.guardian.backend.domain.user.GuardianUser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenGenerator(
    @Value("\${guardian.security.jwt.secret}")
    private val jwtSecret: String
) : TokenGenerator {

    override fun generateToken(user: GuardianUser): String {
        val key = SecretKeySpec(jwtSecret.toByteArray(), SignatureAlgorithm.HS256.jcaName)

        return Jwts.builder()
            .setSubject(user.id.toString())
            .claim("role", user.role.name)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
            .signWith(key)
            .compact()
    }
}
