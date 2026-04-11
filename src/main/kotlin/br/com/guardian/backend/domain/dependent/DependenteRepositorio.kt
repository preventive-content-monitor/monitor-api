package br.com.guardian.backend.domain.dependent

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DependenteRepositorio : JpaRepository<Dependente, UUID> {
    fun findAllByUsuarioGuardianId(usuarioGuardianId: UUID): List<Dependente>
}