package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.persistencia.ClassificacaoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.EventoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.DependenteRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.VulnerabilidadeRepositorio
import br.com.guardian.backend.dominio.modelo.VulnerabilidadeDiaria
import org.springframework.stereotype.Service
import java.time.*
import java.util.*
import kotlin.math.min

@Service
class ServicoVulnerabilidade(
    private val eventoRepositorio: EventoRepositorio,
    private val classificacaoRepositorio: ClassificacaoRepositorio,
    private val dependenteRepositorio: DependenteRepositorio,
    private val vulnerabilidadeRepositorio: VulnerabilidadeRepositorio
) {

    fun calcularDiario(dependenteId: UUID, dia: LocalDate): VulnerabilidadeDiaria {

        val inicio = dia.atStartOfDay().toInstant(ZoneOffset.UTC)
        val fim = dia.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        val eventos = eventoRepositorio.findAll()
            .filter { it.dispositivo.dependente.id == dependenteId }
            .filter { it.ocorridoEm.isAfter(inicio) && it.ocorridoEm.isBefore(fim) }

        val sensiveis = eventos.count {
            val c = classificacaoRepositorio.findByEventoId(it.id)
            c != null && c.pontuacaoRisco >= 70
        }

        val tentativasBloqueio = eventos.count {
            it.tipo.name == "BLOCK_ATTEMPT"
        }

        val usoNoturno = eventos.count {
            val hora = it.ocorridoEm.atZone(ZoneOffset.UTC).hour
            hora in 22..23 || hora in 0..6
        }

        val pontuacaoBruta =
            (0.5 * sensiveis +
                    0.3 * tentativasBloqueio +
                    0.2 * usoNoturno).toInt()

        val pontuacaoFinal = min(pontuacaoBruta, 100)

        val caracteristicas = "sensiveis=$sensiveis,bloqueio=$tentativasBloqueio,noturno=$usoNoturno"

        // Upsert: atualizar se já existe, criar se não existe
        val existente = vulnerabilidadeRepositorio.findByDependenteIdAndDia(dependenteId, dia)
        if (existente != null) {
            existente.pontuacao = pontuacaoFinal
            existente.caracteristicas = caracteristicas
            return vulnerabilidadeRepositorio.save(existente)
        }

        val dependente = dependenteRepositorio.findById(dependenteId)
            .orElseThrow()

        val entidade = VulnerabilidadeDiaria(
            dependente = dependente,
            dia = dia,
            pontuacao = pontuacaoFinal,
            caracteristicas = caracteristicas
        )

        return vulnerabilidadeRepositorio.save(entidade)
    }

    fun buscarHistorico(dependenteId: UUID, from: LocalDate, to: LocalDate) =
        vulnerabilidadeRepositorio.findAllByDependenteIdAndDiaBetween(dependenteId, from, to)
}