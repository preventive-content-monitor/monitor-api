package br.com.guardian.backend.adaptadores.entrada.dto

import br.com.guardian.backend.dominio.modelo.TipoEvento
import java.time.Instant

data class ItemIngestaoEventoDto(
    val tipo: TipoEvento,
    val url: String,
    val titulo: String? = null,
    val ocorridoEm: Instant,
    val metadados: Map<String, Any>? = null
)