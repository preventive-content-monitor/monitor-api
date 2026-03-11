package br.com.guardian.backend.domain.policy

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PolicyRepository : JpaRepository<Policy, UUID> {
    fun findByDependentId(dependentId: UUID): Policy?
}