package br.com.guardian.backend.dominio.modelo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sessoes_credenciais")
class SessaoCredencial(
    @Id
    val id: UUID,

    @Column(name = "sujeito_id", nullable = false)
    val sujeitoId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 24)
    val tipo: TipoCredencial,

    @Column(name = "expira_em", nullable = false)
    val expiraEm: Instant,

    @Column(name = "revogada_em")
    var revogadaEm: Instant? = null
)
