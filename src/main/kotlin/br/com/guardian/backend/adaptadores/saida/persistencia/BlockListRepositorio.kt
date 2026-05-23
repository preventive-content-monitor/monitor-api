package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.BlockList
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface BlockListRepositorio : JpaRepository<BlockList, UUID> {

    fun findByUrlConteudo(urlConteudo: String): BlockList?

    /** Todos os conteúdos com pontuação de risco >= minScore (para exportar blackList.json) */
    @Query("SELECT b FROM BlockList b WHERE b.pontuacaoRisco >= :minScore ORDER BY b.urlConteudo")
    fun findAllBloqueados(@Param("minScore") minScore: Int): List<BlockList>

    /** Todos os conteúdos marcados como seguros (para exportar whiteList.json) */
    @Query("SELECT b FROM BlockList b WHERE b.rotulo = 'SAFE' ORDER BY b.urlConteudo")
    fun findAllSeguros(): List<BlockList>
}
