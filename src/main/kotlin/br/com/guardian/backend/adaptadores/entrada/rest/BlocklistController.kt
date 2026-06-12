package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaVerificacaoBlocklist
import br.com.guardian.backend.adaptadores.saida.classificacao.ClienteOpenAi
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoPolitica
import br.com.guardian.backend.aplicacao.servico.ServicoBlocklist
import br.com.guardian.backend.aplicacao.servico.ServicoIngestaoEvento
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/blocklist")
@Tag(name = "BlockList", description = "Verificação de URLs contra a blocklist global")
class BlocklistController(
    private val servicoBlocklist: ServicoBlocklist,
    private val servicoPolitica: ServicoPolitica,
    private val clienteOpenAi: ClienteOpenAi,
    private val dispositivoRepositorio: DispositivoRepositorio
) {

    private val logger = LoggerFactory.getLogger(BlocklistController::class.java)

    /**
     * Classifica imediatamente uma URL/título via IA (se não cacheado) e retorna
     * a ação que a extensão deve executar. Usado para plataformas mistas (YouTube, etc.)
     * onde o título do conteúdo só fica disponível após a página carregar.
     *
     * Endpoint público — não requer JWT.
     */
    @PostMapping("/classificar-agora")
    @Operation(
        summary = "Classificar e verificar agora",
        description = "Classifica URL+título via IA (se não cacheado) e retorna se deve bloquear. Usado para plataformas mistas como YouTube."
    )
    fun classificarAgora(
        @RequestBody corpo: Map<String, String>
    ): RespostaVerificacaoBlocklist {
        val url              = corpo["url"]           ?: return RespostaVerificacaoBlocklist(acao = "ALLOW", motivo = "URL ausente")
        val titulo: String   = corpo["titulo"]        ?: ""
        val descricao: String = corpo["descricao"]    ?: ""
        val restricaoEtaria   = corpo["restricaoEtaria"] == "true"
        val categoria: String = corpo["categoria"]    ?: ""
        val dispositivoIdStr = corpo["dispositivoId"] ?: return RespostaVerificacaoBlocklist(acao = "ALLOW", motivo = "dispositivoId ausente")

        val dispositivoId: UUID = try {
            UUID.fromString(dispositivoIdStr)
        } catch (e: Exception) {
            return RespostaVerificacaoBlocklist(acao = "ALLOW", motivo = "dispositivoId invalido")
        }

        val host: String = try {
            URI(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return RespostaVerificacaoBlocklist(acao = "ALLOW", motivo = "URL invalida")

        dispositivoRepositorio.findById(dispositivoId).orElse(null)
            ?: return RespostaVerificacaoBlocklist(acao = "ALLOW", motivo = "Dispositivo nao encontrado")

        val conteudoKey: String = ServicoIngestaoEvento.extrairConteudoKey(host, url)

        // 1. Consulta cache da blocklist
        val cacheHit = servicoBlocklist.buscarPorConteudo(conteudoKey)

        val rotulo: String
        val score: Int
        val justificativa: String

        if (cacheHit != null) {
            logger.info("[ClassificarAgora] Cache HIT conteudo={} rotulo={} score={}", conteudoKey, cacheHit.rotulo, cacheHit.pontuacaoRisco)
            rotulo        = cacheHit.rotulo
            score         = cacheHit.pontuacaoRisco
            justificativa = "Cache: ${cacheHit.rotulo}"
        } else {
            // 2. Cache miss → chama OpenAI diretamente (sem Evento persistido)
            try {
                val tituloParam: String? = titulo.ifBlank { null }
                val resposta = clienteOpenAi.classificar(
                    titulo         = tituloParam,
                    urlCompleta    = url,
                    host           = host,
                    descricao      = descricao.ifBlank { null },
                    restricaoEtaria = restricaoEtaria,
                    categoria      = categoria.ifBlank { null }
                )
                logger.info("[ClassificarAgora] OpenAI conteudo={} rotulo={} score={}", conteudoKey, resposta.rotulo, resposta.pontuacaoRisco)

                // Salva no cache para próximas consultas
                servicoBlocklist.registrar(
                    urlConteudo    = conteudoKey,
                    rotulo         = resposta.rotulo,
                    pontuacaoRisco = resposta.pontuacaoRisco,
                    modelo         = "gpt-4o-mini"
                )

                rotulo        = resposta.rotulo
                score         = resposta.pontuacaoRisco
                justificativa = resposta.justificativa
            } catch (e: Exception) {
                logger.warn("[ClassificarAgora] OpenAI indisponivel para url={}: {}", url, e.message)
                return RespostaVerificacaoBlocklist(acao = "ALLOW", motivo = "Classificacao indisponivel temporariamente")
            }
        }

        if (rotulo == "SAFE") {
            return RespostaVerificacaoBlocklist(acao = "ALLOW", motivo = "Conteudo seguro", pontuacaoRisco = score, rotulo = rotulo)
        }

        val politica = servicoPolitica.buscarPoliticaPorDispositivo(dispositivoId)
        return if (score >= politica.limiteRisco || rotulo == "EXPLICIT") {
            RespostaVerificacaoBlocklist(
                acao           = politica.modo.name,
                motivo         = "Conteudo $rotulo (score=$score): $justificativa",
                pontuacaoRisco = score,
                rotulo         = rotulo
            )
        } else {
            RespostaVerificacaoBlocklist(
                acao           = "ALLOW",
                motivo         = "Abaixo do limite ($score < ${politica.limiteRisco})",
                pontuacaoRisco = score,
                rotulo         = rotulo
            )
        }
    }

    /**
     * Verifica se uma URL está bloqueada/liberada com base na política do dispositivo.
     *
     * Retorna a ação que a extensão deve tomar:
     *  - ALLOW   → liberar
     *  - BLOCK   → bloquear (tela de bloqueio)
     *  - WARN    → aviso (o usuário pode optar por continuar)
     *  - EDUCATE → conteúdo educativo substituindo a página
     *  - UNKNOWN → URL ainda não foi analisada pela IA
     */
    @GetMapping("/verificar")
    @Operation(
        summary = "Verificar URL",
        description = "Consulta a blocklist e a política do dispositivo para determinar a ação correta (ALLOW, BLOCK, WARN, EDUCATE, UNKNOWN)."
    )
    fun verificar(
        @RequestParam url: String,
        @RequestParam dispositivoId: UUID
    ): RespostaVerificacaoBlocklist {
        val host = try {
            URI(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return RespostaVerificacaoBlocklist(acao = "ALLOW", motivo = "URL invalida")

        val conteudoKey = ServicoIngestaoEvento.extrairConteudoKey(host, url)
        val entrada = servicoBlocklist.buscarPorConteudo(conteudoKey)
            ?: return RespostaVerificacaoBlocklist(acao = "UNKNOWN", motivo = "Nao analisado ainda")

        if (entrada.rotulo == "SAFE") {
            return RespostaVerificacaoBlocklist(
                acao           = "ALLOW",
                motivo         = "Conteudo seguro",
                pontuacaoRisco = entrada.pontuacaoRisco,
                rotulo         = entrada.rotulo
            )
        }

        val politica = servicoPolitica.buscarPoliticaPorDispositivo(dispositivoId)
        return if (entrada.pontuacaoRisco >= politica.limiteRisco || entrada.rotulo == "EXPLICIT") {
            RespostaVerificacaoBlocklist(
                acao           = politica.modo.name,
                motivo         = "Conteudo ${entrada.rotulo} (score=${entrada.pontuacaoRisco})",
                pontuacaoRisco = entrada.pontuacaoRisco,
                rotulo         = entrada.rotulo
            )
        } else {
            RespostaVerificacaoBlocklist(
                acao           = "ALLOW",
                motivo         = "Abaixo do limite de risco (${entrada.pontuacaoRisco} < ${politica.limiteRisco})",
                pontuacaoRisco = entrada.pontuacaoRisco,
                rotulo         = entrada.rotulo
            )
        }
    }
}
