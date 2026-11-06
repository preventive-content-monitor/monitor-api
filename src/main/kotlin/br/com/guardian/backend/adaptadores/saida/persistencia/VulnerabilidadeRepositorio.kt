package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.VulnerabilidadeDiaria
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

interface VulnerabilidadeRepositorio :
    JpaRepository<VulnerabilidadeDiaria, UUID> {

    fun findByDependenteIdAndDia(dependenteId: UUID, dia: LocalDate): VulnerabilidadeDiaria?

    fun findAllByDependenteIdAndDiaBetween(
        dependenteId: UUID,
        from: LocalDate,
        to: LocalDate
    ): List<VulnerabilidadeDiaria>

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from VulnerabilidadeDiaria v where v.dependente.id = :dependenteId")
    fun excluirPorDependente(@org.springframework.data.repository.query.Param("dependenteId") dependenteId: UUID): Int
}
