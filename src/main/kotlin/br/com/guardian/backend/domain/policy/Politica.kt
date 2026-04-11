package br.com.guardian.backend.domain.policy

import br.com.guardian.backend.domain.dependent.Dependente
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "politicas")
class Politica(

    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id", nullable = false, unique = true)
    val dependente: Dependente,

    @Enumerated(EnumType.STRING)
    @Column(name = "modo", nullable = false)
    var modo: ModoPolitica,

    @Column(name = "limite_risco", nullable = false)
    var limiteRisco: Int,

    @Lob
    @Column(name = "dominios_bloqueados", nullable = false)
    var dominiosBloqueados: String, // JSON simples

    @Lob
    @Column(name = "dominios_permitidos", nullable = false)
    var dominiosPermitidos: String = "[]", // JSON simples

    @Column(name = "modo_escola_ativo", nullable = false)
    var modoEscolaAtivo: Boolean = false,

    @Column(name = "inicio_escola", length = 5)
    var escolaInicio: String? = null, // formato HH:mm

    @Column(name = "fim_escola", length = 5)
    var escolaFim: String? = null, // formato HH:mm

    @Column(name = "criado_em", nullable = false)
    val criadoEm: Instant = Instant.now()
)