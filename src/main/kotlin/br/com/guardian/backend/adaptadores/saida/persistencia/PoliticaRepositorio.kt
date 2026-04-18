package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.Politica
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PoliticaRepositorio : JpaRepository<Politica, UUID> {
    fun findByDependenteId(dependenteId: UUID): Politica?
}