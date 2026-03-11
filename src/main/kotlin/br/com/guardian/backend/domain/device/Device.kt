package br.com.guardian.backend.domain.device

import br.com.guardian.backend.domain.dependent.Dependent
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "devices")
class Device(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_id", nullable = false)
    val dependent: Dependent,

    @Column(nullable = false)
    val deviceName: String,

    @Column(nullable = false)
    val enrolledAt: Instant = Instant.now(),

    var lastSeenAt: Instant? = null
)