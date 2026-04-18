package br.com.guardian.backend.adaptadores.entrada.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.*

@Schema(description = "Dados resumidos de um dispositivo")
data class RespostaDispositivo(

    @Schema(description = "ID único do dispositivo")
    val id: UUID,

    @Schema(description = "Nome do dispositivo", example = "Chrome - Windows")
    val nome: String,

    @Schema(description = "Plataforma do dispositivo", example = "Windows")
    val plataforma: String,

    @Schema(description = "ID do dependente vinculado")
    val dependenteId: UUID,

    @Schema(description = "Apelido do dependente", example = "João")
    val apelidoDependente: String,

    @Schema(description = "Data/hora de vinculação")
    val vinculadoEm: Instant,

    @Schema(description = "Última vez que o dispositivo foi visto")
    val ultimoAcessoEm: Instant?
)
