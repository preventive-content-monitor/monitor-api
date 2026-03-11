package br.com.guardian.backend.domain.events

import br.com.guardian.backend.api.dto.EventBatchIngestRequest
import br.com.guardian.backend.api.dto.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/events")
@Tag(name = "Eventos", description = "Ingestão de eventos de navegação dos dispositivos")
class EventIngestController(
    private val ingestService: EventIngestService
) {

    @PostMapping("/batch")
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
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Dados inválidos na requisição",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun batch(@RequestBody request: EventBatchIngestRequest): Map<String, Any> {
        val count = ingestService.ingestBatch(request)
        return mapOf("status" to "ok", "ingested" to count)
    }
}