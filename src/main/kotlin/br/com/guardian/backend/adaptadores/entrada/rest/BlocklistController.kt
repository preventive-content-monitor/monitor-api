package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaVerificacaoBlocklist
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoPolitica
import br.com.guardian.backend.aplicacao.servico.ServicoBlocklist
import br.com.guardian.backend.aplicacao.servico.ServicoIngestaoEvento
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/blocklist")
@Tag(name = "BlockList", description = "Verificação de URLs contra a blocklist global")
class BlocklistController(
    private val servicoBlocklist: ServicoBlocklist,
    private val servicoPolitica: ServicoPolitica
) {

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
                acao = "ALLOW",
                motivo = "Conteudo seguro",
                pontuacaoRisco = entrada.pontuacaoRisco,
                rotulo = entrada.rotulo
            )
        }

        val politica = servicoPolitica.buscarPoliticaPorDispositivo(dispositivoId)
        return if (entrada.pontuacaoRisco >= politica.limiteRisco || entrada.rotulo == "EXPLICIT") {
            RespostaVerificacaoBlocklist(
                acao = politica.modo.name,
                motivo = "Conteudo ${entrada.rotulo} (score=${entrada.pontuacaoRisco})",
                pontuacaoRisco = entrada.pontuacaoRisco,
                rotulo = entrada.rotulo
            )
        } else {
            RespostaVerificacaoBlocklist(
                acao = "ALLOW",
                motivo = "Abaixo do limite de risco (${entrada.pontuacaoRisco} < ${politica.limiteRisco})",
                pontuacaoRisco = entrada.pontuacaoRisco,
                rotulo = entrada.rotulo
            )
        }
    }
}
