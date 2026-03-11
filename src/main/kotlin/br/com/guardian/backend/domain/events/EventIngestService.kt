package br.com.guardian.backend.domain.events

import br.com.guardian.backend.api.dto.EventBatchIngestRequest
import br.com.guardian.backend.domain.classification.ClassificationService
import br.com.guardian.backend.domain.device.DeviceRepository
import br.com.guardian.backend.domain.metrics.VulnerabilityService
import br.com.guardian.backend.domain.policy.PolicyService
import br.com.guardian.backend.exception.DeviceNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.net.URI
import java.security.MessageDigest
import java.time.ZoneOffset

@Service
class EventIngestService(
    private val deviceRepository: DeviceRepository,
    private val eventRepository: EventRepository,
    private val objectMapper: ObjectMapper,
    private val classificationService: ClassificationService,
    private val policyService: PolicyService,
    private val vulnerabilityService: VulnerabilityService
) {

    fun ingestBatch(request: EventBatchIngestRequest): Int {

        val device = deviceRepository.findById(request.deviceId)
            .orElseThrow { DeviceNotFoundException() }

        val entities = request.events.map { dto ->
            val (host, pathHash) = extractHostAndPathHash(dto.url)

            Event(
                device = device,
                type = dto.type,
                urlHost = host,
                urlPathHash = pathHash,
                title = dto.title,
                occurredAt = dto.occurredAt,
                metadata = dto.metadata?.let { objectMapper.writeValueAsString(it) }
            )
        }

        val saved = eventRepository.saveAll(entities)

        saved.forEach { event ->
            val classification = classificationService.classify(event)

            val shouldBlock = policyService.shouldBlock(
                domain = event.urlHost,
                riskScore = classification.riskScore,
                deviceId = device.id
            )

            if (shouldBlock) {
                println("⚠ BLOQUEAR: ${event.urlHost} score=${classification.riskScore}")
            }
        }

        device.lastSeenAt = java.time.Instant.now()
        deviceRepository.save(device)

        // Recalcular vulnerabilidade para os dias dos eventos
        val dependentId = device.dependent.id
        val daysAffected = saved.map { it.occurredAt.atZone(ZoneOffset.UTC).toLocalDate() }.distinct()
        daysAffected.forEach { day ->
            vulnerabilityService.calculateDaily(dependentId, day)
        }

        return saved.size
    }

    private fun extractHostAndPathHash(url: String): Pair<String, String?> {
        val uri = URI(url)
        val host = uri.host ?: throw RuntimeException("URL sem host: $url")

        val path = uri.path?.takeIf { it.isNotBlank() } ?: return host to null
        val normalized = path.lowercase()

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }

        return host to hash
    }
}