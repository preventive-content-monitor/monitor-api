package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.ResultadoClassificacao
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface ClassificacaoRepositorio : JpaRepository<ResultadoClassificacao, UUID> {

    fun findByEventoId(eventoId: UUID): ResultadoClassificacao?

    // Cache por domínio (sites não-plataforma: urlConteudo == urlHost)
    @Query(
        "SELECT r FROM ResultadoClassificacao r WHERE r.urlHost = :host " +
        "AND r.criadoEm > :desde ORDER BY r.criadoEm DESC"
    )
    fun findPrimeiraRecente(
        @Param("host") host: String,
        @Param("desde") desde: Instant,
        pageable: Pageable
    ): List<ResultadoClassificacao>

    // Cache por conteúdo granular (plataformas mistas: youtube.com/watch?v=ID, vimeo.com/123, etc.)
    @Query(
        "SELECT r FROM ResultadoClassificacao r WHERE r.urlConteudo = :urlConteudo " +
        "AND r.criadoEm > :desde ORDER BY r.criadoEm DESC"
    )
    fun findPrimeiraRecentePorConteudo(
        @Param("urlConteudo") urlConteudo: String,
        @Param("desde") desde: Instant,
        pageable: Pageable
    ): List<ResultadoClassificacao>
}