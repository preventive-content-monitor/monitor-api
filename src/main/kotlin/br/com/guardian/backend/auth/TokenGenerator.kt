package br.com.guardian.backend.auth

import br.com.guardian.backend.domain.user.GuardianUser

interface TokenGenerator {
    fun generateToken(user: GuardianUser): String
}
