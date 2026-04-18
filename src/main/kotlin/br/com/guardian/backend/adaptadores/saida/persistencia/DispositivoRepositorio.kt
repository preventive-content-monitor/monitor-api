package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.Dispositivo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface DispositivoRepositorio : JpaRepository<Dispositivo, UUID> {
    fun findAllByDependenteId(dependenteId: UUID): List<Dispositivo>

    @Query("SELECT d FROM Dispositivo d WHERE d.dependente.usuarioGuardian.id = :usuarioGuardianId")
    fun buscarTodosPorUsuarioGuardianId(usuarioGuardianId: UUID): List<Dispositivo>
}