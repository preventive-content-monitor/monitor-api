package br.com.guardian.backend.domain.classification

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ClassificationRepository : JpaRepository<ClassificationResult, UUID> {
    fun findByEventId(eventId: UUID): ClassificationResult?
}