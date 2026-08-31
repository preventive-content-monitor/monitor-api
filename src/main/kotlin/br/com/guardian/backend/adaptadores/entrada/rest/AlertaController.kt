package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.entrada.dto.*
import br.com.guardian.backend.adaptadores.saida.persistencia.UsuarioRepositorio
import br.com.guardian.backend.aplicacao.servico.ServicoAlerta
import br.com.guardian.backend.aplicacao.servico.ServicoResumoDiario
import br.com.guardian.backend.dominio.modelo.Alerta
import br.com.guardian.backend.dominio.modelo.TipoAlerta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/alertas")
@Tag(name = "Alertas", description = "Central de alertas e preferências de notificação por email")
@SecurityRequirement(name = "bearerAuth")
class AlertaController(
    private val servicoAlerta: ServicoAlerta,
    private val servicoResumoDiario: ServicoResumoDiario,
    private val usuarioRepositorio: UsuarioRepositorio
) {

    @GetMapping
    @Operation(
        summary = "Listar alertas",
        description = "Alertas do responsável autenticado, mais recentes primeiro."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Alertas retornados",
                content = [Content(schema = Schema(implementation = RespostaPaginaAlertas::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun listar(
        @AuthenticationPrincipal principal: Jwt,
        @Parameter(description = "Índice da página (base 0)")
        @RequestParam(defaultValue = "0") pagina: Int,
        @Parameter(description = "Itens por página")
        @RequestParam(defaultValue = "20") tamanho: Int,
        @Parameter(description = "Retornar apenas alertas não lidos")
        @RequestParam(defaultValue = "false") apenasNaoLidos: Boolean,
        @Parameter(description = "Filtrar por tipo de alerta")
        @RequestParam(required = false) tipo: TipoAlerta?
    ): RespostaPaginaAlertas {

        val usuarioId = UUID.fromString(principal.subject)
        val pageable  = PageRequest.of(pagina.coerceAtLeast(0), tamanho.coerceIn(1, 100))

        val resultado = servicoAlerta.listar(usuarioId, apenasNaoLidos, tipo, pageable)

        return RespostaPaginaAlertas(
            itens        = resultado.content.map(::paraResposta),
            pagina       = resultado.number,
            tamanho      = resultado.size,
            totalItens   = resultado.totalElements,
            totalPaginas = resultado.totalPages,
            naoLidos     = servicoAlerta.contarNaoLidos(usuarioId)
        )
    }

    @PatchMapping("/{alertaId}/lido")
    @Operation(summary = "Marcar alerta como lido")
    fun marcarComoLido(
        @AuthenticationPrincipal principal: Jwt,
        @PathVariable alertaId: UUID
    ): ResponseEntity<Void> {
        val usuarioId = UUID.fromString(principal.subject)
        return if (servicoAlerta.marcarComoLido(usuarioId, alertaId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @PatchMapping("/lidos")
    @Operation(summary = "Marcar todos como lidos")
    fun marcarTodosComoLidos(
        @AuthenticationPrincipal principal: Jwt
    ): Map<String, Int> {
        val usuarioId = UUID.fromString(principal.subject)
        return mapOf("marcados" to servicoAlerta.marcarTodosComoLidos(usuarioId))
    }

    // ================= Preferências =================

    @GetMapping("/preferencias")
    @Operation(
        summary = "Buscar preferências de notificação",
        description = "Retorna as escolhas do responsável sobre quando ser notificado por email. " +
            "Criadas com os padrões no primeiro acesso."
    )
    fun buscarPreferencias(
        @AuthenticationPrincipal principal: Jwt
    ): RespostaPreferenciasNotificacao {
        val usuarioId = UUID.fromString(principal.subject)
        val preferencias = servicoAlerta.buscarOuCriarPreferencias(usuarioId)

        return RespostaPreferenciasNotificacao(
            tentativasBloqueio = preferencias.tentativasBloqueio,
            conteudoSensivel   = preferencias.conteudoSensivel,
            resumoDiario       = preferencias.resumoDiario,
            emailDestino       = emailDoUsuario(usuarioId),
            emailHabilitado    = servicoAlerta.emailHabilitado
        )
    }

    @PutMapping("/preferencias")
    @Operation(
        summary = "Atualizar preferências de notificação",
        description = "Define quais alertas geram email. Alertas continuam sendo registrados " +
            "no painel independentemente destas escolhas."
    )
    fun atualizarPreferencias(
        @AuthenticationPrincipal principal: Jwt,
        @RequestBody requisicao: RequisicaoPreferenciasNotificacao
    ): RespostaPreferenciasNotificacao {
        val usuarioId = UUID.fromString(principal.subject)

        val preferencias = servicoAlerta.atualizarPreferencias(
            usuarioId          = usuarioId,
            tentativasBloqueio = requisicao.tentativasBloqueio,
            conteudoSensivel   = requisicao.conteudoSensivel,
            resumoDiario       = requisicao.resumoDiario
        )

        return RespostaPreferenciasNotificacao(
            tentativasBloqueio = preferencias.tentativasBloqueio,
            conteudoSensivel   = preferencias.conteudoSensivel,
            resumoDiario       = preferencias.resumoDiario,
            emailDestino       = emailDoUsuario(usuarioId),
            emailHabilitado    = servicoAlerta.emailHabilitado
        )
    }

    @PostMapping("/preferencias/testar-email")
    @Operation(
        summary = "Enviar email de teste",
        description = "Envia um email de teste para o endereço da conta, validando a configuração SMTP do servidor."
    )
    fun testarEmail(
        @AuthenticationPrincipal principal: Jwt
    ): ServicoAlerta.ResultadoEnvioTeste {
        val usuarioId = UUID.fromString(principal.subject)
        return servicoAlerta.enviarEmailTeste(usuarioId)
    }

    @PostMapping("/preferencias/enviar-resumo")
    @Operation(
        summary = "Enviar resumo diário agora",
        description = "Dispara imediatamente o resumo do dia anterior, sem esperar o envio automático das 8h."
    )
    fun enviarResumoAgora(
        @AuthenticationPrincipal principal: Jwt
    ): Map<String, Any> {
        val usuarioId = UUID.fromString(principal.subject)

        if (!servicoAlerta.emailHabilitado) {
            return mapOf(
                "enviado"  to false,
                "mensagem" to "O servidor de email não está configurado neste ambiente."
            )
        }

        val preferencias = servicoAlerta.buscarOuCriarPreferencias(usuarioId)
        if (!preferencias.resumoDiario) {
            return mapOf(
                "enviado"  to false,
                "mensagem" to "Ative a opção \"Resumo diário\" antes de enviar."
            )
        }

        val enviado = servicoResumoDiario.enviarResumoAgora(usuarioId)
        return mapOf(
            "enviado"  to enviado,
            "mensagem" to if (enviado) {
                "Resumo enviado para ${emailDoUsuario(usuarioId)}."
            } else {
                "Não foi possível enviar. Verifique as credenciais SMTP do servidor."
            }
        )
    }

    // ================= Helpers =================

    private fun emailDoUsuario(usuarioId: UUID): String =
        usuarioRepositorio.findById(usuarioId).map { it.email }.orElse("")

    private fun paraResposta(alerta: Alerta) = RespostaAlerta(
        id             = alerta.id,
        tipo           = alerta.tipo.name,
        severidade     = alerta.severidade.name,
        titulo         = alerta.titulo,
        mensagem       = alerta.mensagem,
        dependente     = alerta.dependente?.apelido,
        referencia     = alerta.referencia,
        pontuacaoRisco = alerta.pontuacaoRisco,
        lido           = alerta.lido,
        emailEnviado   = alerta.emailEnviado,
        criadoEm       = alerta.criadoEm
    )
}
