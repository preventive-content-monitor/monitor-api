package br.com.guardian.backend.domain.policy

import br.com.guardian.backend.api.dto.ErrorResponse
import br.com.guardian.backend.api.dto.UpdatePolicyRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/policy")
@Tag(name = "Políticas", description = "Gerenciamento de políticas de controle parental")
@SecurityRequirement(name = "bearerAuth")
class PolicyController(
    private val policyService: PolicyService
) {

    @GetMapping("/current")
    @Operation(
        summary = "Obter política atual",
        description = "Retorna a política de controle parental ativa para o dispositivo. Se não existir, cria uma política padrão baseada na idade do dependente."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Política retornada com sucesso",
                content = [Content(schema = Schema(implementation = Policy::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dispositivo não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun getCurrent(
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam deviceId: UUID
    ): Policy {
        return policyService.getPolicyByDevice(deviceId)
    }

    @PutMapping
    @Operation(
        summary = "Atualizar política",
        description = "Atualiza a política de controle parental do dispositivo. Permite alterar modo, limite de risco, lista de domínios bloqueados/permitidos e configurações de modo escola."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Política atualizada com sucesso",
                content = [Content(schema = Schema(implementation = Policy::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Dados inválidos na requisição",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dispositivo ou política não encontrada",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun updatePolicy(
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam deviceId: UUID,
        @RequestBody request: UpdatePolicyRequest
    ): Policy {
        return policyService.updatePolicy(
            deviceId = deviceId,
            mode = request.mode,
            riskThreshold = request.riskThreshold,
            blockedDomains = request.blockedDomains,
            allowedDomains = request.allowedDomains,
            schoolModeEnabled = request.schoolModeEnabled,
            schoolStart = request.schoolStart,
            schoolEnd = request.schoolEnd
        )
    }
}