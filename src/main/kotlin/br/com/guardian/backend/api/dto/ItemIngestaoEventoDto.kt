package br.com.guardian.backend.api.dto

import br.com.guardian.backend.domain.events.TipoEvento
import java.time.Instant

data class ItemIngestaoEventoDto(
    val tipo: TipoEvento,
    val url: String,
    val titulo: String? = null,
    val ocorridoEm: Instant,
    val metadados: Map<String, Any>? = null
)