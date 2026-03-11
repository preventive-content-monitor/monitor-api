package br.com.guardian.backend.domain.metrics

import br.com.guardian.backend.domain.events.EventRepository
import br.com.guardian.backend.domain.classification.ClassificationRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class MetricsService(
    private val eventRepository: EventRepository,
    private val classificationRepository: ClassificationRepository
) {

    fun summary(deviceId: UUID, from: Instant, to: Instant): Map<String, Any> {

        val events = eventRepository
            .findAllByDeviceIdAndOccurredAtBetween(deviceId, from, to)

        val total = events.size

        val sensitive = events.count {
            val c = classificationRepository.findByEventId(it.id)
            c != null && c.riskScore >= 70
        }

        val blockAttempts = events.count {
            it.type.name == "BLOCK_ATTEMPT"
        }

        return mapOf(
            "totalEvents" to total,
            "sensitiveEvents" to sensitive,
            "blockAttempts" to blockAttempts
        )
    }

    fun topDomains(deviceId: UUID, from: Instant, to: Instant) =
        eventRepository.topHosts(deviceId, from, to)
}