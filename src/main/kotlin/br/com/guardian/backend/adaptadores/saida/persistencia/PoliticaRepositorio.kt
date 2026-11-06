package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.Politica
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PoliticaRepositorio : JpaRepository<Politica, UUID> {
    fun findByDependenteId(dependenteId: UUID): Politica?

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Politica p where p.dependente.id = :dependenteId")
    fun excluirPorDependente(@org.springframework.data.repository.query.Param("dependenteId") dependenteId: UUID): Int
}
