package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.AuthResponse
import br.com.guardian.backend.api.dto.LoginRequest
import br.com.guardian.backend.api.dto.RegisterRequest

interface AuthenticationService {
    fun register(request: RegisterRequest)
    fun login(request: LoginRequest): AuthResponse
}
