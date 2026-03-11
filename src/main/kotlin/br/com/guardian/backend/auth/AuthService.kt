package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.AuthResponse
import br.com.guardian.backend.api.dto.LoginRequest
import br.com.guardian.backend.api.dto.RegisterRequest
import br.com.guardian.backend.domain.user.GuardianUser
import br.com.guardian.backend.domain.user.UserRepository
import br.com.guardian.backend.exception.EmailAlreadyExistsException
import br.com.guardian.backend.exception.InvalidCredentialsException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${guardian.security.jwt.secret}")
    private val jwtSecret: String
) {

    fun register(request: RegisterRequest) {
        if (userRepository.findByEmail(request.email) != null) {
            throw EmailAlreadyExistsException()
        }

        val user = GuardianUser(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
        )

        userRepository.save(user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val key = SecretKeySpec(jwtSecret.toByteArray(), SignatureAlgorithm.HS256.jcaName)

        val token = Jwts.builder()
            .setSubject(user.id.toString())
            .claim("role", user.role.name)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
            .signWith(key)
            .compact()

        return AuthResponse(token)
    }
}