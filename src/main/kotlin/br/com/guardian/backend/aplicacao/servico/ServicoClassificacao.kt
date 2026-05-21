package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.classificacao.ClienteOpenAi
import br.com.guardian.backend.adaptadores.saida.persistencia.ClassificacaoRepositorio
import br.com.guardian.backend.dominio.modelo.Evento
import br.com.guardian.backend.dominio.modelo.ResultadoClassificacao
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class ServicoClassificacao(
    private val clienteOpenAi: ClienteOpenAi,
    private val classificacaoRepositorio: ClassificacaoRepositorio
) {
    private val logger = LoggerFactory.getLogger(ServicoClassificacao::class.java)

    companion object {
        private val CACHE_TTL = Duration.ofHours(24)
    }

    fun classificar(evento: Evento, urlCompleta: String): ResultadoClassificacao {
        val desde = Instant.now().minus(CACHE_TTL)
        val cacheHit = classificacaoRepositorio.findPrimeiraRecente(evento.urlHost, desde)

        if (cacheHit != null) {
            logger.info("[Cache HIT] host={} rotulo={} score={}", evento.urlHost, cacheHit.rotulo, cacheHit.pontuacaoRisco)
            // Cria registro para este evento com valores do cache
            return classificacaoRepositorio.save(
                ResultadoClassificacao(
                    evento = evento,
                    urlHost = evento.urlHost,
                    modelo = cacheHit.modelo + "-cached",
                    rotulo = cacheHit.rotulo,
                    pontuacaoRisco = cacheHit.pontuacaoRisco,
                    justificativa = cacheHit.justificativa
                )
            )
        }

        val resposta = clienteOpenAi.classificar(evento.titulo, urlCompleta, evento.urlHost)

        return classificacaoRepositorio.save(
            ResultadoClassificacao(
                evento = evento,
                urlHost = evento.urlHost,
                modelo = "gpt-4o-mini",
                rotulo = resposta.rotulo,
                pontuacaoRisco = resposta.pontuacaoRisco,
                justificativa = resposta.justificativa
            )
        )
    }
}