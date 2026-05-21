package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.ResultadoClassificacao
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface ClassificacaoRepositorio : JpaRepository<ResultadoClassificacao, UUID> {

    fun findByEventoId(eventoId: UUID): ResultadoClassificacao?

    @Query(
        "SELECT r FROM ResultadoClassificacao r WHERE r.urlHost = :host " +
        "AND r.criadoEm > :desde ORDER BY r.criadoEm DESC"
    )
    fun findPrimeiraRecente(
        @Param("host") host: String,
        @Param("desde") desde: Instant
    ): ResultadoClassificacao?
}