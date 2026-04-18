package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.ResultadoClassificacao
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ClassificacaoRepositorio : JpaRepository<ResultadoClassificacao, UUID> {
    fun findByEventoId(eventoId: UUID): ResultadoClassificacao?
}