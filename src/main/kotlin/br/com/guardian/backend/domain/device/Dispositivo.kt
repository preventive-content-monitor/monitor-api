package br.com.guardian.backend.domain.device

import br.com.guardian.backend.domain.dependent.Dependente
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "dispositivos")
class Dispositivo(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id", nullable = false)
    val dependente: Dependente,

    @Column(name = "nome_dispositivo", nullable = false)
    val nomeDispositivo: String,

    @Column(name = "vinculado_em", nullable = false)
    val vinculadoEm: Instant = Instant.now(),

    @Column(name = "ultimo_acesso_em")
    var ultimoAcessoEm: Instant? = null
)