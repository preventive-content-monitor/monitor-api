package br.com.guardian.backend.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.*

@Schema(description = "Dados resumidos de um dispositivo")
data class DeviceResponse(

    @Schema(description = "ID único do dispositivo")
    val id: UUID,

    @Schema(description = "Nome do dispositivo", example = "Chrome - Windows")
    val name: String,

    @Schema(description = "Plataforma do dispositivo", example = "Windows")
    val platform: String,

    @Schema(description = "ID do dependente vinculado")
    val dependentId: UUID,

    @Schema(description = "Apelido do dependente", example = "João")
    val dependentNickname: String,

    @Schema(description = "Data/hora de vinculação")
    val enrolledAt: Instant,

    @Schema(description = "Última vez que o dispositivo foi visto")
    val lastSeenAt: Instant?
)
