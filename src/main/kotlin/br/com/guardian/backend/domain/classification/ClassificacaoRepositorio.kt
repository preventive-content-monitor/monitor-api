package br.com.guardian.backend.domain.classification

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ClassificacaoRepositorio : JpaRepository<ResultadoClassificacao, UUID> {
    fun findByEventoId(eventoId: UUID): ResultadoClassificacao?
}