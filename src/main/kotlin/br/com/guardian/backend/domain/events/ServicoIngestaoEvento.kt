package br.com.guardian.backend.domain.events

import br.com.guardian.backend.api.dto.RequisicaoIngestaoLoteEvento
import br.com.guardian.backend.domain.classification.ServicoClassificacao
import br.com.guardian.backend.domain.device.DispositivoRepositorio
import br.com.guardian.backend.domain.metrics.ServicoVulnerabilidade
import br.com.guardian.backend.domain.policy.ServicoPolitica
import br.com.guardian.backend.exception.DispositivoNaoEncontradoExcecao
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.net.URI
import java.security.MessageDigest
import java.time.ZoneOffset

@Service
class ServicoIngestaoEvento(
    private val dispositivoRepositorio: DispositivoRepositorio,
    private val eventoRepositorio: EventoRepositorio,
    private val objectMapper: ObjectMapper,
    private val servicoClassificacao: ServicoClassificacao,
    private val servicoPolitica: ServicoPolitica,
    private val servicoVulnerabilidade: ServicoVulnerabilidade
) {

    fun ingerirLote(requisicao: RequisicaoIngestaoLoteEvento): Int {

        val dispositivo = dispositivoRepositorio.findById(requisicao.dispositivoId)
            .orElseThrow { DispositivoNaoEncontradoExcecao() }

        val entidades = requisicao.eventos.map { dto ->
            val (host, pathHash) = extrairHostEPathHash(dto.url)

            Evento(
                dispositivo = dispositivo,
                tipo = dto.tipo,
                urlHost = host,
                urlPathHash = pathHash,
                titulo = dto.titulo,
                ocorridoEm = dto.ocorridoEm,
                metadados = dto.metadados?.let { objectMapper.writeValueAsString(it) }
            )
        }

        val salvos = eventoRepositorio.saveAll(entidades)

        salvos.forEach { evento ->
            val classificacao = servicoClassificacao.classificar(evento)

            val deveBloquear = servicoPolitica.deveBloquear(
                dominio = evento.urlHost,
                pontuacaoRisco = classificacao.pontuacaoRisco,
                dispositivoId = dispositivo.id
            )

            if (deveBloquear) {
                println("⚠ BLOQUEAR: ${evento.urlHost} score=${classificacao.pontuacaoRisco}")
            }
        }

        dispositivo.ultimoAcessoEm = java.time.Instant.now()
        dispositivoRepositorio.save(dispositivo)

        // Recalcular vulnerabilidade para os dias dos eventos
        val dependenteId = dispositivo.dependente.id
        val diasAfetados = salvos.map { it.ocorridoEm.atZone(ZoneOffset.UTC).toLocalDate() }.distinct()
        diasAfetados.forEach { dia ->
            servicoVulnerabilidade.calcularDiario(dependenteId, dia)
        }

        return salvos.size
    }

    private fun extrairHostEPathHash(url: String): Pair<String, String?> {
        val uri = URI(url)
        val host = uri.host ?: throw RuntimeException("URL sem host: $url")

        val path = uri.path?.takeIf { it.isNotBlank() } ?: return host to null
        val normalizado = path.lowercase()

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalizado.toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }

        return host to hash
    }
}