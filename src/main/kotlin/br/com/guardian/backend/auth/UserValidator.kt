package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.LoginRequest
import br.com.guardian.backend.domain.user.GuardianUser

interface UserValidator {
    fun validateRegistration(email: String)
    fun validateLogin(request: LoginRequest): GuardianUser
}
