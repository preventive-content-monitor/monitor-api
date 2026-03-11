package br.com.guardian.backend.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<GuardianUser, UUID> {
    fun findByEmail(email: String): GuardianUser?
}