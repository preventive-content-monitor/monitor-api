package br.com.guardian.backend.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UsuarioRepositorio : JpaRepository<UsuarioGuardian, UUID> {
    fun findByEmail(email: String): UsuarioGuardian?
}