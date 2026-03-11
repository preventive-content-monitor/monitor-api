package br.com.guardian.backend.api.dto

import java.util.*

data class EventBatchIngestRequest(
    val deviceId: UUID,
    val events: List<EventIngestItemDto>
)