package br.com.guardian.backend.domain.events

import br.com.guardian.backend.domain.device.Dispositivo
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "eventos")
class Evento(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispositivo_id", nullable = false)
    val dispositivo: Dispositivo,

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    val tipo: TipoEvento,

    @Column(name = "url_host", nullable = false, length = 255)
    val urlHost: String,

    @Column(name = "url_path_hash", length = 64)
    val urlPathHash: String? = null,

    @Column(name = "titulo", length = 512)
    val titulo: String? = null,

    @Column(name = "ocorrido_em", nullable = false)
    val ocorridoEm: Instant,

    @Lob
    @Column(name = "metadados")
    val metadados: String? = null
)