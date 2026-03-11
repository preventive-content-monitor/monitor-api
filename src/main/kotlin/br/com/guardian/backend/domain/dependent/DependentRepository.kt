package br.com.guardian.backend.domain.dependent

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DependentRepository : JpaRepository<Dependent, UUID> {
    fun findAllByGuardianUserId(guardianUserId: UUID): List<Dependent>
}