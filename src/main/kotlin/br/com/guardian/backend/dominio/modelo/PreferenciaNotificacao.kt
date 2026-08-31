package br.com.guardian.backend.dominio.modelo

import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * Escolhas do responsável sobre quando quer ser notificado por email.
 *
 * Criada com os padrões abaixo no primeiro acesso à tela de Alertas: os dois
 * avisos de segurança vêm ligados, e o resumo diário desligado por ser o único
 * que gera email mesmo quando não há nada de errado.
 */
@Entity
@Table(name = "preferencias_notificacao")
class PreferenciaNotificacao(

    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_guardian_id", nullable = false, unique = true)
    val usuarioGuardian: UsuarioGuardian,

    @Column(name = "tentativas_bloqueio", nullable = false)
    var tentativasBloqueio: Boolean = true,

    @Column(name = "conteudo_sensivel", nullable = false)
    var conteudoSensivel: Boolean = true,

    @Column(name = "resumo_diario", nullable = false)
    var resumoDiario: Boolean = false,

    @Column(name = "atualizado_em", nullable = false)
    var atualizadoEm: Instant = Instant.now()
) {
    /** Traduz o tipo de alerta na preferência que governa o envio do email. */
    fun permiteEmailPara(tipo: TipoAlerta): Boolean = when (tipo) {
        TipoAlerta.TENTATIVA_BLOQUEIO -> tentativasBloqueio
        TipoAlerta.CONTEUDO_SENSIVEL  -> conteudoSensivel
        TipoAlerta.RESUMO_DIARIO      -> resumoDiario
        // Vinculação de dispositivo é informativa e parte do fluxo que o próprio
        // responsável iniciou — aparece no painel, mas não dispara email.
        TipoAlerta.NOVO_DISPOSITIVO   -> false
    }
}
