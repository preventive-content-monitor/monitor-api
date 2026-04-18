package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.UsuarioGuardian
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UsuarioRepositorio : JpaRepository<UsuarioGuardian, UUID> {
    fun findByEmail(email: String): UsuarioGuardian?
}