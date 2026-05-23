package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoIngestaoLoteEvento
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.EventoRepositorio
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoPolitica
import br.com.guardian.backend.dominio.excecao.DispositivoNaoEncontradoExcecao
import br.com.guardian.backend.dominio.modelo.Evento
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
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

    companion object {
        private val logger = LoggerFactory.getLogger(ServicoIngestaoEvento::class.java)

        private fun ehPlataformaMista(host: String): Boolean {
            val h = host.lowercase()
            return h in ServicoBlocklistS3.PLATAFORMAS_MISTAS ||
                ServicoBlocklistS3.PLATAFORMAS_MISTAS.any { plat ->
                    h.endsWith("." + plat.removePrefix("www."))
                }
        }

        /**
         * Retorna a chave de conteúdo para uso em S3 e cache de classificação.
         *
         * - Sites normais  → domínio puro  (ex: "pornhub.com")
         * - Plataformas    → URL do conteúdo específico:
         *     YouTube  → "youtube.com/watch?v=VIDEO_ID"
         *     youtu.be → "youtube.com/watch?v=VIDEO_ID"
         *     Outros   → "host/path"  (ex: "vimeo.com/123456")
         *
         * Isso permite bloquear/liberar um vídeo sem afetar o domínio inteiro.
         */
        fun extrairConteudoKey(host: String, urlOriginal: String): String {
            if (!ehPlataformaMista(host)) return host
            return try {
                val uri = URI(urlOriginal)
                val hostNorm = host.lowercase().removePrefix("www.")
                val path = uri.path?.takeIf { it.isNotBlank() && it != "/" }

                when {
                    hostNorm == "youtube.com" -> {
                        val videoId = uri.query?.split("&")
                            ?.find { it.startsWith("v=") }
                            ?.removePrefix("v=")
                            ?.takeIf { it.isNotBlank() }
                        if (videoId != null) "youtube.com/watch?v=$videoId" else host
                    }
                    hostNorm == "youtu.be" -> {
                        // https://youtu.be/VIDEO_ID
                        path?.let { "youtube.com/watch?v=${it.removePrefix("/")}" } ?: host
                    }
                    path != null -> "$hostNorm$path"
                    else -> host
                }
            } catch (e: Exception) { host }
        }
    }

    fun ingerirLote(requisicao: RequisicaoIngestaoLoteEvento): Int {

        val dispositivo = dispositivoRepositorio.findById(requisicao.dispositivoId)
            .orElseThrow { DispositivoNaoEncontradoExcecao() }

        // Filtra URLs que não são http/https (ex: edge://, chrome://, about:, etc.)
        val eventosValidos = requisicao.eventos.filter { dto ->
            val valido = dto.url.startsWith("http://") || dto.url.startsWith("https://")
            if (!valido) {
                logger.debug("[Ingestao] URL ignorada (esquema nao-http): {}", dto.url)
            }
            valido
        }

        if (eventosValidos.isEmpty()) {
            logger.debug("[Ingestao] Nenhum evento http/https no lote — ignorando classificacao")
            return 0
        }

        val pares: List<Pair<String, Evento>> = eventosValidos.map { dto ->
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
            // Chave de conteúdo granular: domínio para sites normais,
            // URL do conteúdo específico para plataformas (ex: youtube.com/watch?v=VIDEO_ID)
            val conteudoKey = extrairConteudoKey(evento.urlHost, urlOriginal)
            val classificacao = servicoClassificacao.classificar(evento, urlOriginal, conteudoKey)

            val deveBloquear = servicoPolitica.deveBloquear(
                dominio = evento.urlHost,
                pontuacaoRisco = classificacao.pontuacaoRisco,
                dispositivoId = dispositivo.id
            )

            if (deveBloquear) {
                servicoPolitica.adicionarDominioBloqueado(evento.urlHost, dispositivo.id)
            }

            // S3: usa conteudoKey → para plataformas, armazena o conteúdo específico
            // (ex: "youtube.com/watch?v=BAD_VIDEO") e não o domínio inteiro
            if (classificacao.pontuacaoRisco >= 70 || deveBloquear) {
                servicoBlocklistS3.adicionarAoBlacklist(conteudoKey)
            } else if (classificacao.rotulo == "SAFE") {
                servicoBlocklistS3.adicionarAoWhitelist(conteudoKey)
            }

            if (ehPlataformaMista(evento.urlHost)) {
                logger.debug(
                    "[Ingestao] Plataforma mista: host={} conteudoKey={} score={}",
                    evento.urlHost, conteudoKey, classificacao.pontuacaoRisco
                )
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
