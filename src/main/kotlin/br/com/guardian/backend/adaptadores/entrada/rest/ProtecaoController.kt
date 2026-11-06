package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.aplicacao.servico.ServicoProtecao
import br.com.guardian.backend.dominio.excecao.NaoAutorizadoExcecao
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.UUID

data class RequisicaoPalavraChave(val palavraChave: String = "")
data class RequisicaoSinal(val versao: String? = null, val anonimoPermitido: Boolean? = null)

@RestController
@RequestMapping("/api/protecao")
@Tag(name = "Proteção", description = "Palavra-chave, sinal de vida e detecção de remoção da extensão")
@SecurityRequirement(name = "bearerAuth")
class ProtecaoController(private val servico: ServicoProtecao) {

    @GetMapping("/palavra-chave")
    @Operation(summary = "Informa se a conta já tem palavra-chave")
    fun status(@AuthenticationPrincipal principal: Jwt): Map<String, Boolean> =
        mapOf("definida" to servico.palavraChaveDefinida(usuarioId(principal)))

    @PutMapping("/palavra-chave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Define a palavra-chave",
        description = "O responsável pode alterar a qualquer momento; sessões de vinculação só podem criar a primeira."
    )
    fun definir(@AuthenticationPrincipal principal: Jwt, @RequestBody requisicao: RequisicaoPalavraChave) {
        val podeSubstituir = principal.getClaimAsString("scope") == TipoCredencial.RESPONSAVEL.escopo
        servico.definirPalavraChave(usuarioId(principal), requisicao.palavraChave, podeSubstituir)
    }

    @PostMapping("/verificar")
    @Operation(summary = "Confere a palavra-chave digitada na extensão")
    fun verificar(@AuthenticationPrincipal principal: Jwt, @RequestBody requisicao: RequisicaoPalavraChave) =
        servico.verificar(UUID.fromString(principal.subject), requisicao.palavraChave)

    @PostMapping("/desconectar")
    @Operation(summary = "Desconecta a extensão após confirmar a palavra-chave")
    fun desconectar(@AuthenticationPrincipal principal: Jwt, @RequestBody requisicao: RequisicaoPalavraChave) =
        servico.desconectar(UUID.fromString(principal.subject), requisicao.palavraChave)

    @PostMapping("/sinal")
    @Operation(summary = "Sinal de vida da extensão")
    fun sinal(@AuthenticationPrincipal principal: Jwt, @RequestBody requisicao: RequisicaoSinal): Map<String, Any> {
        val dispositivoId = UUID.fromString(principal.subject)
        val token = servico.registrarSinal(dispositivoId, requisicao.versao, requisicao.anonimoPermitido)
        return mapOf(
            "tokenRemocao" to token,
            "palavraChaveDefinida" to servico.palavraChaveDefinida(servico.usuarioDoDispositivo(dispositivoId))
        )
    }

    /** Aberto pelo Chrome quando a extensão é removida (chrome.runtime.setUninstallURL). */
    @GetMapping("/removida")
    @Operation(summary = "Registra a remoção da extensão e mostra a página informativa")
    fun removida(@RequestParam d: String, @RequestParam t: String): ResponseEntity<Void> {
        val id = try { UUID.fromString(d) } catch (e: IllegalArgumentException) { null }
        if (id != null) servico.registrarRemocao(id, t)
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/removido.html")).build()
    }

    private fun usuarioId(principal: Jwt): UUID {
        val sujeito = UUID.fromString(principal.subject)
        return when (principal.getClaimAsString("scope")) {
            TipoCredencial.DISPOSITIVO.escopo -> servico.usuarioDoDispositivo(sujeito)
            TipoCredencial.RESPONSAVEL.escopo, TipoCredencial.CONFIGURACAO.escopo,
            TipoCredencial.VINCULACAO.escopo -> sujeito
            else -> throw NaoAutorizadoExcecao("Sessão sem permissão.")
        }
    }
}
