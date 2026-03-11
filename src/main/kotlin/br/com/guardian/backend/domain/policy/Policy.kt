package br.com.guardian.backend.domain.policy

import br.com.guardian.backend.domain.dependent.Dependent
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "policies")
class Policy(

    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_id", nullable = false, unique = true)
    val dependent: Dependent,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var mode: PolicyMode,

    @Column(nullable = false)
    var riskThreshold: Int,

    @Lob
    @Column(nullable = false)
    var blockedDomains: String, // JSON simples

    @Lob
    @Column(nullable = false)
    var allowedDomains: String = "[]", // JSON simples

    @Column(nullable = false)
    var schoolModeEnabled: Boolean = false,

    @Column(length = 5)
    var schoolStart: String? = null, // formato HH:mm

    @Column(length = 5)
    var schoolEnd: String? = null, // formato HH:mm

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)