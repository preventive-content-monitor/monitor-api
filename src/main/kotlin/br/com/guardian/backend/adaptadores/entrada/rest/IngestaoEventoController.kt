package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoIngestaoLoteEvento
import br.com.guardian.backend.adaptadores.entrada.dto.RespostaErro
import br.com.guardian.backend.aplicacao.servico.ServicoIngestaoEvento
import br.com.guardian.backend.aplicacao.servico.ServicoAutorizacaoDispositivo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Ingestão de eventos de navegação dos dispositivos")
class IngestaoEventoController(
    private val servicoIngestao: ServicoIngestaoEvento,
    private val autorizacao: ServicoAutorizacaoDispositivo
) {

    @PostMapping("/lote")
    @Operation(
        summary = "Ingestão em lote",
        description = "Recebe um lote de eventos de navegação de um dispositivo. Os eventos são classificados automaticamente e verificados contra as políticas ativas."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Eventos processados com sucesso"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dispositivo não encontrado",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Dados inválidos na requisição",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun lote(
        @RequestBody requisicao: RequisicaoIngestaoLoteEvento,
        @AuthenticationPrincipal principal: Jwt
    ): Map<String, Any> {
        autorizacao.verificar(requisicao.dispositivoId, principal)
        val quantidade = servicoIngestao.ingerirLote(requisicao)
        return mapOf("status" to "ok", "ingeridos" to quantidade)
    }
}