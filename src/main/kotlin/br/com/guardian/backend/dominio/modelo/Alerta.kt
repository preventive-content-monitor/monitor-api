package br.com.guardian.backend.dominio.modelo

import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * Notificação gerada pelo sistema para o responsável.
 *
 * O alerta é sempre persistido — aparece na Central de Alertas do painel
 * independentemente das preferências. O que a preferência controla é apenas o
 * envio do email correspondente, registrado em [emailEnviado].
 */
@Entity
@Table(
    name = "alertas",
    indexes = [
        Index(name = "idx_alerta_usuario_criado", columnList = "usuario_guardian_id, criado_em")
    ]
)
class Alerta(

    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_guardian_id", nullable = false)
    val usuarioGuardian: UsuarioGuardian,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id")
    val dependente: Dependente? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    val tipo: TipoAlerta,

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade", nullable = false, length = 20)
    val severidade: SeveridadeAlerta,

    @Column(name = "titulo", nullable = false, length = 200)
    val titulo: String,

    @Column(name = "mensagem", nullable = false, length = 1000)
    val mensagem: String,

    /** Domínio ou chave de conteúdo que originou o alerta, quando aplicável. */
    @Column(name = "referencia", length = 500)
    val referencia: String? = null,

    @Column(name = "pontuacao_risco")
    val pontuacaoRisco: Int? = null,

    @Column(name = "lido", nullable = false)
    var lido: Boolean = false,

    @Column(name = "email_enviado", nullable = false)
    var emailEnviado: Boolean = false,

    @Column(name = "criado_em", nullable = false)
    val criadoEm: Instant = Instant.now()
)
