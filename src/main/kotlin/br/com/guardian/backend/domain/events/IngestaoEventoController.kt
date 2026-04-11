package br.com.guardian.backend.domain.events

import br.com.guardian.backend.api.dto.RequisicaoIngestaoLoteEvento
import br.com.guardian.backend.api.dto.RespostaErro
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Ingestão de eventos de navegação dos dispositivos")
class IngestaoEventoController(
    private val servicoIngestao: ServicoIngestaoEvento
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
    fun lote(@RequestBody requisicao: RequisicaoIngestaoLoteEvento): Map<String, Any> {
        val quantidade = servicoIngestao.ingerirLote(requisicao)
        return mapOf("status" to "ok", "ingeridos" to quantidade)
    }
}