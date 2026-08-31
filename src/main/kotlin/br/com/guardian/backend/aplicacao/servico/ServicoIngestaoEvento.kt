package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoIngestaoLoteEvento
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.EventoRepositorio
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoPolitica
import br.com.guardian.backend.dominio.excecao.DispositivoNaoEncontradoExcecao
import br.com.guardian.backend.dominio.modelo.Evento
import br.com.guardian.backend.dominio.modelo.SeveridadeAlerta
import br.com.guardian.backend.dominio.modelo.TipoAlerta
import br.com.guardian.backend.dominio.modelo.TipoEvento
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
    private val servicoBlocklist: ServicoBlocklist,
    private val servicoAlerta: ServicoAlerta
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

            // Plataformas mistas (YouTube, Twitch, etc.) nunca têm o domínio inteiro bloqueado —
            // apenas o conteúdo específico é marcado na block_list (cache da classificação).
            // Bloquear "youtube.com" derrubaria todos os vídeos, incluindo os seguros.
            val ehPlataformaMistaEvento = ehPlataformaMista(evento.urlHost)

            val deveBloquear = !ehPlataformaMistaEvento && servicoPolitica.deveBloquear(
                dominio = evento.urlHost,
                pontuacaoRisco = classificacao.pontuacaoRisco,
                dispositivoId = dispositivo.id
            )

            if (deveBloquear) {
                servicoPolitica.adicionarDominioBloqueado(evento.urlHost, dispositivo.id)
            }

            // Grava classificação na tabela block_list (fonte de verdade).
            // O ServicoExportacaoS3 sincroniza o MySQL → S3 a cada 5 minutos.
            servicoBlocklist.registrar(
                urlConteudo = conteudoKey,
                rotulo = classificacao.rotulo,
                pontuacaoRisco = classificacao.pontuacaoRisco,
                modelo = classificacao.modelo
            )

            if (ehPlataformaMista(evento.urlHost)) {
                logger.debug(
                    "[Ingestao] Plataforma mista: host={} conteudoKey={} score={}",
                    evento.urlHost, conteudoKey, classificacao.pontuacaoRisco
                )
            }

            notificarSeNecessario(evento, dispositivo, conteudoKey, classificacao.pontuacaoRisco, classificacao.rotulo)
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

    /**
     * Gera alerta para o responsável quando o acesso merece atenção.
     *
     * Dois gatilhos, correspondentes às duas preferências de notificação:
     *  - evento BLOCK_ATTEMPT       → tentativa de burlar um bloqueio (crítico)
     *  - score acima do limite      → conteúdo sensível detectado (atenção)
     *
     * A deduplicação por conteúdo fica no ServicoAlerta, então recarregar a
     * mesma página bloqueada não gera uma enxurrada de emails.
     */
    private fun notificarSeNecessario(
        evento: Evento,
        dispositivo: br.com.guardian.backend.dominio.modelo.Dispositivo,
        conteudoKey: String,
        pontuacaoRisco: Int,
        rotulo: String
    ) {
        val dependente = dispositivo.dependente
        val usuario    = dependente.usuarioGuardian

        if (evento.tipo == TipoEvento.BLOCK_ATTEMPT) {
            servicoAlerta.registrar(
                usuario        = usuario,
                dependente     = dependente,
                tipo           = TipoAlerta.TENTATIVA_BLOQUEIO,
                severidade     = SeveridadeAlerta.CRITICO,
                titulo         = "Tentativa de acesso bloqueado",
                mensagem       = "${dependente.apelido} tentou acessar ${evento.urlHost}, que está bloqueado pela política do dispositivo.",
                referencia     = conteudoKey,
                pontuacaoRisco = pontuacaoRisco
            )
            return
        }

        val limite = try {
            servicoPolitica.buscarPoliticaPorDispositivo(dispositivo.id).limiteRisco
        } catch (e: Exception) {
            logger.debug("[Alerta] Politica indisponivel para dispositivo={}", dispositivo.id)
            return
        }

        if (rotulo != "SAFE" && pontuacaoRisco >= limite) {
            servicoAlerta.registrar(
                usuario        = usuario,
                dependente     = dependente,
                tipo           = TipoAlerta.CONTEUDO_SENSIVEL,
                severidade     = if (pontuacaoRisco >= 85) SeveridadeAlerta.CRITICO else SeveridadeAlerta.ATENCAO,
                titulo         = "Conteúdo sensível detectado",
                mensagem       = "${dependente.apelido} acessou conteúdo classificado como $rotulo em ${evento.urlHost}.",
                referencia     = conteudoKey,
                pontuacaoRisco = pontuacaoRisco
            )
        }
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
