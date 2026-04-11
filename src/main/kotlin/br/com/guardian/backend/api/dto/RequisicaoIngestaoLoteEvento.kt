package br.com.guardian.backend.api.dto

import java.util.*

data class RequisicaoIngestaoLoteEvento(
    val dispositivoId: UUID,
    val eventos: List<ItemIngestaoEventoDto>
)