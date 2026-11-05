package br.com.guardian.backend.dominio.modelo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@JsonIgnoreProperties("hibernateLazyInitializer", "handler")
@Entity
@Table(name = "usuarios_guardian")
class UsuarioGuardian(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(name = "hash_senha", nullable = false)
    @get:JsonIgnore
    var senhaHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false)
    val papel: PapelUsuario = PapelUsuario.RESPONSAVEL,

    @Column(name = "criado_em", nullable = false)
    val criadoEm: Instant = Instant.now()
)