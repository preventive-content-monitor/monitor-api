package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.Dependente
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DependenteRepositorio : JpaRepository<Dependente, UUID> {
    fun findAllByUsuarioGuardianId(usuarioGuardianId: UUID): List<Dependente>
}