package br.com.guardian.backend.domain.user

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "usuarios_guardian")
class UsuarioGuardian(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(name = "hash_senha", nullable = false)
    var senhaHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false)
    val papel: PapelUsuario = PapelUsuario.RESPONSAVEL,

    @Column(name = "criado_em", nullable = false)
    val criadoEm: Instant = Instant.now()
)