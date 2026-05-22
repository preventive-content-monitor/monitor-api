package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoIngestaoLoteEvento
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.EventoRepositorio
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoPolitica
import br.com.guardian.backend.dominio.excecao.DispositivoNaoEncontradoExcecao
import br.com.guardian.backend.dominio.modelo.Evento
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
    private val servicoVulnerabilidade: ServicoVulnerabilidade,
    private val servicoBlocklistS3: ServicoBlocklistS3
) {

    fun ingerirLote(requisicao: RequisicaoIngestaoLoteEvento): Int {

        val dispositivo = dispositivoRepositorio.findById(requisicao.dispositivoId)
            .orElseThrow { DispositivoNaoEncontradoExcecao() }

        val pares: List<Pair<String, Evento>> = requisicao.eventos.map { dto ->
            val (host, pathHash) = extrairHostEPathHash(dto.url)

            dto.url to Evento(
                dispositivo = dispositivo,
                tipo = dto.tipo,
                urlHost = host,
                urlPathHash = pathHash,
                titulo = dto.titulo,
                ocorridoEm = dto.ocorridoEm,
                metadados = dto.metadados?.let { objectMapper.writeValueAsString(it) }
            )
        }

        val salvos = eventoRepositorio.saveAll(pares.map { it.second })

        salvos.forEachIndexed { idx, evento ->
            val urlOriginal = pares[idx].first
            val classificacao = servicoClassificacao.classificar(evento, urlOriginal)

            val deveBloquear = servicoPolitica.deveBloquear(
                dominio = evento.urlHost,
                pontuacaoRisco = classificacao.pontuacaoRisco,
                dispositivoId = dispositivo.id
            )

            if (deveBloquear) {
                servicoPolitica.adicionarDominioBloqueado(evento.urlHost, dispositivo.id)
            }

            // S3 blacklist: sites com risco alto OU qualquer site que a política individual bloqueou
            if (classificacao.pontuacaoRisco >= 70 || deveBloquear) {
                servicoBlocklistS3.adicionarAoBlacklist(evento.urlHost)
            } else if (classificacao.rotulo == "SAFE") {
                servicoBlocklistS3.adicionarAoWhitelist(evento.urlHost)
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