package br.com.guardian.backend.domain.metrics

import br.com.guardian.backend.api.dto.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.time.*
import java.util.*

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Métricas e indicadores para o painel do responsável")
@SecurityRequirement(name = "bearerAuth")
class DashboardController(
    private val metricsService: MetricsService,
    private val vulnerabilityService: VulnerabilityService
) {

    @GetMapping("/summary")
    @Operation(
        summary = "Resumo de métricas",
        description = "Retorna um resumo das métricas de navegação do dispositivo no período especificado, incluindo total de eventos, eventos sensíveis e tentativas de bloqueio."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Resumo retornado com sucesso"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Parâmetros inválidos",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun summary(
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam deviceId: UUID,
        @Parameter(description = "Data/hora de início (ISO 8601)", example = "2026-02-01T00:00:00Z", required = true)
        @RequestParam from: String,
        @Parameter(description = "Data/hora de fim (ISO 8601)", example = "2026-02-22T23:59:59Z", required = true)
        @RequestParam to: String
    ): Map<String, Any> {

        val start = Instant.parse(from)
        val end = Instant.parse(to)

        return metricsService.summary(deviceId, start, end)
    }

    @GetMapping("/top-domains")
    @Operation(
        summary = "Top domínios acessados",
        description = "Retorna os domínios mais acessados pelo dispositivo no período especificado, ordenados por quantidade de acessos."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Lista de domínios retornada com sucesso"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Parâmetros inválidos",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun topDomains(
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam deviceId: UUID,
        @Parameter(description = "Data/hora de início (ISO 8601)", example = "2026-02-01T00:00:00Z", required = true)
        @RequestParam from: String,
        @Parameter(description = "Data/hora de fim (ISO 8601)", example = "2026-02-22T23:59:59Z", required = true)
        @RequestParam to: String
    ) =
        metricsService.topDomains(
            deviceId,
            Instant.parse(from),
            Instant.parse(to)
        )

    @GetMapping("/vulnerability")
    @Operation(
        summary = "Histórico de vulnerabilidade",
        description = "Retorna o histórico diário do índice de vulnerabilidade do dependente. O score varia de 0 a 100, sendo baseado em comportamentos como acesso a conteúdo sensível, tentativas de burlar bloqueios e uso noturno."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Histórico retornado com sucesso",
                content = [Content(array = ArraySchema(schema = Schema(implementation = VulnerabilityDaily::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Parâmetros inválidos",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun vulnerability(
        @Parameter(description = "ID do dependente", required = true)
        @RequestParam dependentId: UUID,
        @Parameter(description = "Data de início (YYYY-MM-DD)", example = "2026-02-01", required = true)
        @RequestParam from: String,
        @Parameter(description = "Data de fim (YYYY-MM-DD)", example = "2026-02-22", required = true)
        @RequestParam to: String
    ) =
        vulnerabilityService.getHistory(
            dependentId,
            LocalDate.parse(from),
            LocalDate.parse(to)
        )
}