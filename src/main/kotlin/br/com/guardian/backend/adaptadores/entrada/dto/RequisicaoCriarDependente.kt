package br.com.guardian.backend.adaptadores.entrada.dto

import br.com.guardian.backend.dominio.modelo.SexoDependente
import java.time.LocalDate

data class RequisicaoCriarDependente(
    val apelido: String,
    val dataNascimento: LocalDate,
    val sexo: SexoDependente
)