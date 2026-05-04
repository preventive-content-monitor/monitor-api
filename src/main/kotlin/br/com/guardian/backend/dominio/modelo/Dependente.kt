package br.com.guardian.backend.dominio.modelo

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
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

    @Column(name = "data_nascimento")
    val dataNascimento: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", length = 10)
    val sexo: SexoDependente? = null,

    @Column(name = "criado_em", nullable = false)
    val criadoEm: Instant = Instant.now()
)