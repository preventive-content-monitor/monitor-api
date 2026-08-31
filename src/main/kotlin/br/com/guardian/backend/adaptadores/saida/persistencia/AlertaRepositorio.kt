package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.Alerta
import br.com.guardian.backend.dominio.modelo.TipoAlerta
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface AlertaRepositorio : JpaRepository<Alerta, UUID> {

    @Query(
        value = """
        select a from Alerta a
        where a.usuarioGuardian.id = :usuarioId
          and (:apenasNaoLidos = false or a.lido = false)
          and (:tipo is null or a.tipo = :tipo)
        order by a.criadoEm desc
    """,
        countQuery = """
        select count(a) from Alerta a
        where a.usuarioGuardian.id = :usuarioId
          and (:apenasNaoLidos = false or a.lido = false)
          and (:tipo is null or a.tipo = :tipo)
    """
    )
    fun buscarPorUsuario(
        @Param("usuarioId") usuarioId: UUID,
        @Param("apenasNaoLidos") apenasNaoLidos: Boolean,
        @Param("tipo") tipo: TipoAlerta?,
        pageable: Pageable
    ): Page<Alerta>

    fun countByUsuarioGuardianIdAndLidoFalse(usuarioId: UUID): Long

    @Modifying
    @Query("update Alerta a set a.lido = true where a.usuarioGuardian.id = :usuarioId and a.lido = false")
    fun marcarTodosComoLidos(@Param("usuarioId") usuarioId: UUID): Int

    /**
     * Impede alertas repetidos para o mesmo conteúdo dentro de uma janela curta.
     * Sem isso, recarregar uma página bloqueada geraria um email a cada tentativa.
     */
    fun existsByUsuarioGuardianIdAndTipoAndReferenciaAndCriadoEmAfter(
        usuarioId: UUID,
        tipo: TipoAlerta,
        referencia: String,
        desde: Instant
    ): Boolean

    /** Alertas do período — alimenta o resumo diário por email. */
    fun findAllByUsuarioGuardianIdAndCriadoEmBetweenOrderByCriadoEmDesc(
        usuarioId: UUID,
        de: Instant,
        ate: Instant
    ): List<Alerta>
}
