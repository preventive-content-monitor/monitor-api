package br.com.guardian.backend.domain.device

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface DeviceRepository : JpaRepository<Device, UUID> {
    fun findAllByDependentId(dependentId: UUID): List<Device>

    @Query("SELECT d FROM Device d WHERE d.dependent.guardianUser.id = :guardianUserId")
    fun findAllByGuardianUserId(guardianUserId: UUID): List<Device>
}