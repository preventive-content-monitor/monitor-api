package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.email.ClienteEmail
import br.com.guardian.backend.adaptadores.saida.email.ModelosEmail
import br.com.guardian.backend.adaptadores.saida.persistencia.AlertaRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.PreferenciaNotificacaoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.UsuarioRepositorio
import br.com.guardian.backend.dominio.modelo.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Cria alertas, decide se o email correspondente deve sair e faz o envio.
 *
 * Regra central: o alerta é SEMPRE persistido e aparece no painel. A preferência
 * do responsável controla apenas o email.
 */
@Service
class ServicoAlerta(
    private val alertaRepositorio: AlertaRepositorio,
    private val preferenciaRepositorio: PreferenciaNotificacaoRepositorio,
    private val usuarioRepositorio: UsuarioRepositorio,
    private val clienteEmail: ClienteEmail,
    private val modelosEmail: ModelosEmail
) {
    private val logger = LoggerFactory.getLogger(ServicoAlerta::class.java)

    companion object {
        /**
         * Janela de deduplicação. Recarregar uma página bloqueada dispara vários
         * eventos; sem isso o responsável receberia um email por recarga.
         */
        private val JANELA_DEDUPE = Duration.ofHours(6)
    }

    // ================= Preferências =================

    /** Busca as preferências do usuário, criando-as com os padrões no primeiro acesso. */
    @Transactional
    fun buscarOuCriarPreferencias(usuarioId: UUID): PreferenciaNotificacao {
        preferenciaRepositorio.findByUsuarioGuardianId(usuarioId)?.let { return it }

        val usuario = usuarioRepositorio.findById(usuarioId)
            .orElseThrow { IllegalArgumentException("Usuario nao encontrado") }

        return preferenciaRepositorio.save(PreferenciaNotificacao(usuarioGuardian = usuario))
    }

    @Transactional
    fun atualizarPreferencias(
        usuarioId: UUID,
        tentativasBloqueio: Boolean,
        conteudoSensivel: Boolean,
        resumoDiario: Boolean
    ): PreferenciaNotificacao {
        val preferencias = buscarOuCriarPreferencias(usuarioId)

        preferencias.tentativasBloqueio = tentativasBloqueio
        preferencias.conteudoSensivel   = conteudoSensivel
        preferencias.resumoDiario       = resumoDiario
        preferencias.atualizadoEm       = Instant.now()

        return preferenciaRepositorio.save(preferencias)
    }

    // ================= Leitura =================

    @Transactional(readOnly = true)
    fun listar(
        usuarioId: UUID,
        apenasNaoLidos: Boolean,
        tipo: TipoAlerta?,
        pageable: Pageable
    ): Page<Alerta> = alertaRepositorio.buscarPorUsuario(usuarioId, apenasNaoLidos, tipo, pageable)

    @Transactional(readOnly = true)
    fun contarNaoLidos(usuarioId: UUID): Long =
        alertaRepositorio.countByUsuarioGuardianIdAndLidoFalse(usuarioId)

    @Transactional
    fun marcarComoLido(usuarioId: UUID, alertaId: UUID): Boolean {
        val alerta = alertaRepositorio.findById(alertaId).orElse(null) ?: return false
        if (alerta.usuarioGuardian.id != usuarioId) return false

        alerta.lido = true
        alertaRepositorio.save(alerta)
        return true
    }

    @Transactional
    fun marcarTodosComoLidos(usuarioId: UUID): Int =
        alertaRepositorio.marcarTodosComoLidos(usuarioId)

    // ================= Criação =================

    /**
     * Registra um alerta e envia o email se a preferência do responsável permitir.
     *
     * Nunca lança: é chamado de dentro do pipeline de ingestão de eventos, e uma
     * falha de notificação não pode impedir o evento de ser salvo.
     */
    @Transactional
    fun registrar(
        usuario: UsuarioGuardian,
        dependente: Dependente?,
        tipo: TipoAlerta,
        severidade: SeveridadeAlerta,
        titulo: String,
        mensagem: String,
        referencia: String? = null,
        pontuacaoRisco: Int? = null
    ): Alerta? {
        return try {
            // Deduplicação por (usuário, tipo, referência) dentro da janela
            if (referencia != null) {
                val jaExiste = alertaRepositorio
                    .existsByUsuarioGuardianIdAndTipoAndReferenciaAndCriadoEmAfter(
                        usuario.id, tipo, referencia, Instant.now().minus(JANELA_DEDUPE)
                    )
                if (jaExiste) {
                    logger.debug("[Alerta] Duplicado ignorado tipo={} ref={}", tipo, referencia)
                    return null
                }
            }

            val alerta = alertaRepositorio.save(
                Alerta(
                    usuarioGuardian = usuario,
                    dependente      = dependente,
                    tipo            = tipo,
                    severidade      = severidade,
                    titulo          = titulo,
                    mensagem        = mensagem,
                    referencia      = referencia,
                    pontuacaoRisco  = pontuacaoRisco
                )
            )

            enviarEmailSePermitido(alerta, usuario, dependente?.apelido)
            alerta
        } catch (e: Exception) {
            logger.error("[Alerta] Falha ao registrar tipo={}: {}", tipo, e.message)
            null
        }
    }

    private fun enviarEmailSePermitido(
        alerta: Alerta,
        usuario: UsuarioGuardian,
        apelidoDependente: String?
    ) {
        if (!clienteEmail.habilitado) return

        val preferencias = buscarOuCriarPreferencias(usuario.id)
        if (!preferencias.permiteEmailPara(alerta.tipo)) {
            logger.debug("[Alerta] Email suprimido por preferencia tipo={}", alerta.tipo)
            return
        }

        val enviado = clienteEmail.enviarHtml(
            destinatario = usuario.email,
            assunto      = "[Guardian] ${alerta.titulo}",
            corpoHtml    = modelosEmail.alertaIndividual(alerta, apelidoDependente)
        )

        if (enviado) {
            alerta.emailEnviado = true
            alertaRepositorio.save(alerta)
        }
    }

    // ================= Teste de configuração =================

    /**
     * Envia um email de teste para o próprio responsável — permite validar a
     * configuração SMTP a partir do painel, sem esperar um incidente real.
     */
    fun enviarEmailTeste(usuarioId: UUID): ResultadoEnvioTeste {
        if (!clienteEmail.habilitado) {
            return ResultadoEnvioTeste(
                enviado = false,
                mensagem = "O servidor de email não está configurado neste ambiente."
            )
        }

        val usuario = usuarioRepositorio.findById(usuarioId).orElse(null)
            ?: return ResultadoEnvioTeste(false, "Usuário não encontrado.")

        val enviado = clienteEmail.enviarHtml(
            destinatario = usuario.email,
            assunto      = "[Guardian] Teste de notificação",
            corpoHtml    = modelosEmail.emailTeste(usuario.email)
        )

        return if (enviado) {
            ResultadoEnvioTeste(true, "Email de teste enviado para ${usuario.email}.")
        } else {
            ResultadoEnvioTeste(false, "Não foi possível enviar. Verifique as credenciais SMTP do servidor.")
        }
    }

    data class ResultadoEnvioTeste(val enviado: Boolean, val mensagem: String)

    val emailHabilitado: Boolean
        get() = clienteEmail.habilitado
}
