package br.com.guardian.backend.dominio.modelo

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "classificacoes")
class ResultadoClassificacao(

    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false, unique = true)
    val evento: Evento,

    @Column(name = "modelo", nullable = false)
    val modelo: String,

    @Column(name = "rotulo", nullable = false)
    val rotulo: String,

    @Column(name = "pontuacao_risco", nullable = false)
    val pontuacaoRisco: Int,

    @Lob
    @Column(name = "justificativa")
    val justificativa: String? = null,

    @Column(name = "criado_em", nullable = false)
    val criadoEm: Instant = Instant.now()
)