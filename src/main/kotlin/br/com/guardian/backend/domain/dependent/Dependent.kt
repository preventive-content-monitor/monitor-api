package br.com.guardian.backend.domain.dependent

import br.com.guardian.backend.domain.user.GuardianUser
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "dependents")
class Dependent(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_user_id", nullable = false)
    val guardianUser: GuardianUser,

    @Column(nullable = false)
    val nickname: String,

    @Column(nullable = false)
    val birthYear: Int,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)