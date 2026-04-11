package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.AuthResponse
import br.com.guardian.backend.api.dto.LoginRequest
import br.com.guardian.backend.api.dto.RegisterRequest
import br.com.guardian.backend.domain.user.GuardianUser
import br.com.guardian.backend.domain.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthenticationServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userValidator: UserValidator,
    private val tokenGenerator: TokenGenerator
) : AuthenticationService {

    override fun register(request: RegisterRequest) {
        userValidator.validateRegistration(request.email)

        val user = GuardianUser(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
        )

        userRepository.save(user)
    }

    override fun login(request: LoginRequest): AuthResponse {
        val user = userValidator.validateLogin(request)
        val token = tokenGenerator.generateToken(user)
        return AuthResponse(token)
    }
}
