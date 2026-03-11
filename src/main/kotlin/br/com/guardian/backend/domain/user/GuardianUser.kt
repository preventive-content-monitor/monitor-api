package br.com.guardian.backend.domain.user

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "guardian_users")
class GuardianUser(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.RESPONSAVEL,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)