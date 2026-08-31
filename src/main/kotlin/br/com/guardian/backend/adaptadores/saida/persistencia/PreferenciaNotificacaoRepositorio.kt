package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.PreferenciaNotificacao
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PreferenciaNotificacaoRepositorio : JpaRepository<PreferenciaNotificacao, UUID> {

    fun findByUsuarioGuardianId(usuarioId: UUID): PreferenciaNotificacao?

    /** Destinatários do resumo diário — usado pelo job agendado. */
    fun findAllByResumoDiarioTrue(): List<PreferenciaNotificacao>
}
