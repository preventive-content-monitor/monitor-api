package br.com.guardian.backend.api.dto

import br.com.guardian.backend.domain.policy.PolicyMode
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern

@Schema(description = "Dados para atualização de política de controle parental")
data class UpdatePolicyRequest(

    @Schema(description = "Modo de operação da política", example = "WARN")
    val mode: PolicyMode,

    @field:Min(0)
    @field:Max(100)
    @Schema(description = "Limite de risco (0-100) para acionar a política", example = "50")
    val riskThreshold: Int,

    @Schema(description = "Lista de domínios bloqueados", example = "[\"facebook.com\", \"tiktok.com\"]")
    val blockedDomains: List<String> = emptyList(),

    @Schema(description = "Lista de domínios permitidos (whitelist)", example = "[\"khanacademy.org\", \"google.com\"]")
    val allowedDomains: List<String> = emptyList(),

    @Schema(description = "Ativa modo escola (restrições em horário escolar)", example = "true")
    val schoolModeEnabled: Boolean = false,

    @field:Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Formato deve ser HH:mm")
    @Schema(description = "Horário de início do modo escola", example = "07:00")
    val schoolStart: String? = null,

    @field:Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Formato deve ser HH:mm")
    @Schema(description = "Horário de fim do modo escola", example = "17:00")
    val schoolEnd: String? = null
)
