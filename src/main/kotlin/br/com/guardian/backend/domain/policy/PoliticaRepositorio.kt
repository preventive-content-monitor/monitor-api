package br.com.guardian.backend.domain.policy

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PoliticaRepositorio : JpaRepository<Politica, UUID> {
    fun findByDependenteId(dependenteId: UUID): Politica?
}