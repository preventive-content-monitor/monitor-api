package br.com.guardian.backend.api.dto

import br.com.guardian.backend.domain.events.EventType
import java.time.Instant

data class EventIngestItemDto(
    val type: EventType,
    val url: String,
    val title: String? = null,
    val occurredAt: Instant,
    val metadata: Map<String, Any>? = null
)