package br.com.guardian.backend.adaptadores.entrada.dto

import java.util.*

data class RequisicaoIngestaoLoteEvento(
    val dispositivoId: UUID,
    val eventos: List<ItemIngestaoEventoDto>
)