package br.com.guardian.backend.domain.dependent

import br.com.guardian.backend.domain.user.UsuarioGuardian
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "dependentes")
class Dependente(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_guardian_id", nullable = false)
    val usuarioGuardian: UsuarioGuardian,

    @Column(name = "apelido", nullable = false)
    val apelido: String,

    @Column(name = "ano_nascimento", nullable = false)
    val anoNascimento: Int,

    @Column(name = "criado_em", nullable = false)
    val criadoEm: Instant = Instant.now()
)