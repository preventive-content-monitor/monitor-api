package br.com.guardian.backend.domain.device

import br.com.guardian.backend.api.dto.DeviceResponse
import br.com.guardian.backend.api.dto.EnrollmentRequest
import br.com.guardian.backend.api.dto.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Dispositivos", description = "Gerenciamento e vinculação de dispositivos dos dependentes")
@SecurityRequirement(name = "bearerAuth")
class DeviceController(
    private val enrollmentService: DeviceEnrollmentService,
    private val deviceRepository: DeviceRepository
) {

    @GetMapping
    @Operation(
        summary = "Listar dispositivos",
        description = "Retorna todos os dispositivos vinculados aos dependentes do usuário autenticado"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Lista de dispositivos retornada com sucesso",
                content = [Content(array = ArraySchema(schema = Schema(implementation = DeviceResponse::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun listDevices(
        @AuthenticationPrincipal principal: org.springframework.security.oauth2.jwt.Jwt
    ): List<DeviceResponse> {
        val userId = UUID.fromString(principal.subject)
        return deviceRepository.findAllByGuardianUserId(userId).map { device ->
            val platform = extractPlatform(device.deviceName)
            DeviceResponse(
                id = device.id,
                name = device.deviceName,
                platform = platform,
                dependentId = device.dependent.id,
                dependentNickname = device.dependent.nickname,
                enrolledAt = device.enrolledAt,
                lastSeenAt = device.lastSeenAt
            )
        }
    }

    private fun extractPlatform(deviceName: String): String {
        return when {
            deviceName.contains("Windows", ignoreCase = true) -> "Windows"
            deviceName.contains("Mac", ignoreCase = true) -> "macOS"
            deviceName.contains("Linux", ignoreCase = true) -> "Linux"
            deviceName.contains("Android", ignoreCase = true) -> "Android"
            deviceName.contains("iOS", ignoreCase = true) || deviceName.contains("iPhone", ignoreCase = true) -> "iOS"
            else -> "Unknown"
        }
    }

    @PostMapping("/generate-code/{dependentId}")
    @Operation(
        summary = "Gerar código de vinculação",
        description = "Gera um código de 6 caracteres válido por 5 minutos para vincular um dispositivo ao dependente"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Código gerado com sucesso"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dependente não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun generateCode(@PathVariable dependentId: UUID): Map<String, String> {
        val code = enrollmentService.generateEnrollmentCode(dependentId)
        return mapOf("code" to code)
    }

    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Vincular dispositivo",
        description = "Vincula um dispositivo ao dependente usando o código de vinculação gerado anteriormente"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Dispositivo vinculado com sucesso",
                content = [Content(schema = Schema(implementation = Device::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Código inválido ou expirado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dependente não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun enroll(@RequestBody request: EnrollmentRequest): Device {
        return enrollmentService.enrollDevice(request.code, request.deviceName)
    }
}