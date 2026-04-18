package br.com.guardian.backend.dominio.modelo

import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "vulnerabilidade_diaria")
class VulnerabilidadeDiaria(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id", nullable = false)
    val dependente: Dependente,

    @Column(name = "dia_ref", nullable = false)
    val dia: LocalDate,

    @Column(name = "pontuacao", nullable = false)
    var pontuacao: Int,

    @Lob
    @Column(name = "caracteristicas")
    var caracteristicas: String? = null
)