package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.SessaoCredencial
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface SessaoCredencialRepositorio : JpaRepository<SessaoCredencial, UUID> {
    @Modifying(flushAutomatically = true)
    @Query("""
        update SessaoCredencial s set s.revogadaEm = :revogadaEm
        where s.sujeitoId = :sujeitoId and s.tipo = :tipo and s.revogadaEm is null
    """)
    fun revogarSessoes(
        @Param("sujeitoId") sujeitoId: UUID,
        @Param("tipo") tipo: TipoCredencial,
        @Param("revogadaEm") revogadaEm: Instant
    ): Int

    @Modifying
    @Query("delete from SessaoCredencial s where s.sujeitoId in :sujeitos")
    fun excluirPorSujeitos(@Param("sujeitos") sujeitos: Collection<UUID>): Int
}
