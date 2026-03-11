package br.com.guardian.backend.domain.events

import br.com.guardian.backend.domain.device.Device
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "events")
class Event(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    val device: Device,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: EventType,

    @Column(name = "url_host", nullable = false, length = 255)
    val urlHost: String,

    @Column(name = "url_path_hash", length = 64)
    val urlPathHash: String? = null,

    @Column(length = 512)
    val title: String? = null,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,

    @Lob
    @Column
    val metadata: String? = null
)