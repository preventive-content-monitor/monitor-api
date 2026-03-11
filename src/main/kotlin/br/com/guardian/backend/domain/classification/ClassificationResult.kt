package br.com.guardian.backend.domain.classification

import br.com.guardian.backend.domain.events.Event
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "classifications")
class ClassificationResult(

    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    val event: Event,

    @Column(nullable = false)
    val model: String,

    @Column(nullable = false)
    val label: String,

    @Column(nullable = false)
    val riskScore: Int,

    @Lob
    val rationale: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)