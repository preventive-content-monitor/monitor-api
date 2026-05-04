package br.com.guardian.backend.adaptadores.entrada.dto

import br.com.guardian.backend.dominio.modelo.ModoPolitica
import br.com.guardian.backend.dominio.modelo.SexoDependente
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Schema(description = "Resposta de política do dependente para um dispositivo")
data class RespostaPolitica(

    @Schema(description = "ID da política")
    val id: UUID,

    @Schema(description = "ID do dependente")
    val dependenteId: UUID,

    @Schema(description = "Nome exibido do dependente")
    val nomeDependente: String,

    @Schema(description = "Ano de nascimento do dependente")
    val anoNascimentoDependente: Int,

    @Schema(description = "Data de nascimento completa do dependente")
    val dataNascimentoDependente: LocalDate?,

    @Schema(description = "Sexo do dependente")
    val sexoDependente: SexoDependente?,

    @Schema(description = "Modo da política")
    val modo: ModoPolitica,

    @Schema(description = "Limite de risco")
    val limiteRisco: Int,

    @Schema(description = "Domínios bloqueados serializados em JSON")
    val dominiosBloqueados: String,

    @Schema(description = "Domínios permitidos serializados em JSON")
    val dominiosPermitidos: String,

    @Schema(description = "Indica se modo escola está ativo")
    val modoEscolaAtivo: Boolean,

    @Schema(description = "Horário de início do modo escola")
    val escolaInicio: String?,

    @Schema(description = "Horário de fim do modo escola")
    val escolaFim: String?,

    @Schema(description = "Data/hora de criação da política")
    val criadoEm: Instant
)