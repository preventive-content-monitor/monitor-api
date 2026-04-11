package br.com.guardian.backend.domain.metrics

import br.com.guardian.backend.domain.events.EventoRepositorio
import br.com.guardian.backend.domain.classification.ClassificacaoRepositorio
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class ServicoMetricas(
    private val eventoRepositorio: EventoRepositorio,
    private val classificacaoRepositorio: ClassificacaoRepositorio
) {

    fun resumo(dispositivoId: UUID, from: Instant, to: Instant): Map<String, Any> {

        val eventos = eventoRepositorio
            .findAllByDispositivoIdAndOcorridoEmBetween(dispositivoId, from, to)

        val total = eventos.size

        val sensiveis = eventos.count {
            val c = classificacaoRepositorio.findByEventoId(it.id)
            c != null && c.pontuacaoRisco >= 70
        }

        val tentativasBloqueio = eventos.count {
            it.tipo.name == "BLOCK_ATTEMPT"
        }

        return mapOf(
            "totalEventos" to total,
            "eventosSensiveis" to sensiveis,
            "tentativasBloqueio" to tentativasBloqueio
        )
    }

    fun topDominios(dispositivoId: UUID, from: Instant, to: Instant) =
        eventoRepositorio.topHosts(dispositivoId, from, to)
}