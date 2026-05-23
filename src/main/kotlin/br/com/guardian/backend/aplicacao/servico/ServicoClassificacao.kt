package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.classificacao.ClienteClassificador
import br.com.guardian.backend.adaptadores.saida.classificacao.ClienteOpenAi
import br.com.guardian.backend.adaptadores.saida.persistencia.ClassificacaoRepositorio
import br.com.guardian.backend.dominio.modelo.Evento
import br.com.guardian.backend.dominio.modelo.ResultadoClassificacao
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class ServicoClassificacao(
    private val clienteOpenAi: ClienteOpenAi,
    private val clienteClassificador: ClienteClassificador,
    private val servicoBlocklist: ServicoBlocklist,
    private val classificacaoRepositorio: ClassificacaoRepositorio
) {
    private val logger = LoggerFactory.getLogger(ServicoClassificacao::class.java)

    companion object {
        private val CACHE_TTL = Duration.ofHours(24)
    }

    /**
     * @param conteudoKey chave granular de conteúdo:
     *   - Sites normais: igual a urlHost (ex: "pornhub.com")
     *   - Plataformas mistas: URL do conteúdo específico (ex: "youtube.com/watch?v=VIDEO_ID")
     * Usado para cache 24h e operações S3 — evita que um vídeo seguro no YouTube
     * faça o cache retornar SAFE para todos os outros vídeos do mesmo domínio.
     */
    fun classificar(evento: Evento, urlCompleta: String, conteudoKey: String): ResultadoClassificacao {
        val desde = Instant.now().minus(CACHE_TTL)

        // Cache lookup por conteúdo granular (não por domínio)
        val cacheHit = classificacaoRepositorio
            .findPrimeiraRecentePorConteudo(conteudoKey, desde, PageRequest.of(0, 1))
            .firstOrNull()

        if (cacheHit != null) {
            logger.info("[Cache HIT] conteudo={} rotulo={} score={}", conteudoKey, cacheHit.rotulo, cacheHit.pontuacaoRisco)
            val modeloBase = cacheHit.modelo.removeSuffix("-cached")
            return classificacaoRepositorio.save(
                ResultadoClassificacao(
                    evento = evento,
                    urlHost = evento.urlHost,
                    urlConteudo = conteudoKey,
                    modelo = "$modeloBase-cached",
                    rotulo = cacheHit.rotulo,
                    pontuacaoRisco = cacheHit.pontuacaoRisco,
                    justificativa = cacheHit.justificativa
                )
            )
        }

        try {
            val resposta = clienteOpenAi.classificar(evento.titulo, urlCompleta, evento.urlHost)
            return classificacaoRepositorio.save(
                ResultadoClassificacao(
                    evento = evento,
                    urlHost = evento.urlHost,
                    urlConteudo = conteudoKey,
                    modelo = "gpt-4o-mini",
                    rotulo = resposta.rotulo,
                    pontuacaoRisco = resposta.pontuacaoRisco,
                    justificativa = resposta.justificativa
                )
            )
        } catch (e: Exception) {
            logger.warn("[Classificacao] OpenAI indisponivel para conteudo={}, verificando blocklist no DB...", conteudoKey)

            // 1º fallback: blocklist DB — conteúdo já classificado como perigoso anteriormente
            if (servicoBlocklist.estaNaBlacklist(conteudoKey)) {
                logger.info("[Classificacao] conteudo={} encontrado na blocklist (DB)", conteudoKey)
                return classificacaoRepositorio.save(
                    ResultadoClassificacao(
                        evento = evento,
                        urlHost = evento.urlHost,
                        urlConteudo = conteudoKey,
                        modelo = "db-blacklist-fallback",
                        rotulo = "EXPLICIT",
                        pontuacaoRisco = 90,
                        justificativa = "Conteudo presente na blocklist (DB) — OpenAI indisponivel"
                    )
                )
            }

            // 2º fallback: classificador local por palavras-chave
            logger.info("[Classificacao] conteudo={} nao encontrado no DB, usando classificador local", conteudoKey)
            val (rotulo, score) = clienteClassificador.classificar(evento.titulo, evento.urlHost)
            return classificacaoRepositorio.save(
                ResultadoClassificacao(
                    evento = evento,
                    urlHost = evento.urlHost,
                    urlConteudo = conteudoKey,
                    modelo = "keyword-fallback",
                    rotulo = rotulo,
                    pontuacaoRisco = score,
                    justificativa = "Classificação local por palavras-chave (OpenAI indisponível)"
                )
            )
        }
    }
}