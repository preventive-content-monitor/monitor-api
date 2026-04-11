package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.LoginRequest
import br.com.guardian.backend.domain.user.GuardianUser
import br.com.guardian.backend.domain.user.UserRepository
import br.com.guardian.backend.exception.EmailAlreadyExistsException
import br.com.guardian.backend.exception.InvalidCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class UserValidatorImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserValidator {

    override fun validateRegistration(email: String) {
        if (userRepository.findByEmail(email) != null) {
            throw EmailAlreadyExistsException()
        }
    }

    override fun validateLogin(request: LoginRequest): GuardianUser {
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        return user
    }
}
