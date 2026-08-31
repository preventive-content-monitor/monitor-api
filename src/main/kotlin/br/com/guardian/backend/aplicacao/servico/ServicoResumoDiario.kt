package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.email.ClienteEmail
import br.com.guardian.backend.adaptadores.saida.email.ModelosEmail
import br.com.guardian.backend.adaptadores.saida.persistencia.AlertaRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.PreferenciaNotificacaoRepositorio
import br.com.guardian.backend.dominio.modelo.TipoAlerta
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

/**
 * Envia o resumo diário para os responsáveis que ativaram a opção.
 *
 * Roda toda manhã às 8h (horário de São Paulo) e consolida os alertas do dia
 * anterior. Diferente dos alertas individuais, o resumo é enviado mesmo quando
 * não houve incidentes — a ausência de problemas também é informação útil.
 */
@Service
class ServicoResumoDiario(
    private val preferenciaRepositorio: PreferenciaNotificacaoRepositorio,
    private val alertaRepositorio: AlertaRepositorio,
    private val clienteEmail: ClienteEmail,
    private val modelosEmail: ModelosEmail
) {
    private val logger = LoggerFactory.getLogger(ServicoResumoDiario::class.java)

    companion object {
        private val FUSO = ZoneId.of("America/Sao_Paulo")
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    fun enviarResumosDiarios() {
        if (!clienteEmail.habilitado) {
            logger.debug("[ResumoDiario] SMTP nao configurado — job ignorado")
            return
        }

        val ontem = LocalDate.now(FUSO).minusDays(1)
        val enviados = enviarResumosPara(ontem)

        logger.info("[ResumoDiario] {} resumos enviados referentes a {}", enviados, ontem)
    }

    /** Separado do @Scheduled para poder ser disparado manualmente pelo painel. */
    @Transactional(readOnly = true)
    fun enviarResumosPara(dia: LocalDate): Int {
        val inicio = dia.atStartOfDay(FUSO).toInstant()
        val fim    = dia.plusDays(1).atStartOfDay(FUSO).toInstant()

        var enviados = 0

        preferenciaRepositorio.findAllByResumoDiarioTrue().forEach { preferencia ->
            val usuario = preferencia.usuarioGuardian

            try {
                // O próprio resumo não entra na contagem do resumo seguinte
                val alertas = alertaRepositorio
                    .findAllByUsuarioGuardianIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                        usuario.id, inicio, fim
                    )
                    .filter { it.tipo != TipoAlerta.RESUMO_DIARIO }

                val ok = clienteEmail.enviarHtml(
                    destinatario = usuario.email,
                    assunto      = "[Guardian] Resumo diário — ${alertas.size} ${if (alertas.size == 1) "alerta" else "alertas"}",
                    corpoHtml    = modelosEmail.resumoDiario(dia, alertas)
                )

                if (ok) enviados++
            } catch (e: Exception) {
                logger.error("[ResumoDiario] Falha para usuario={}: {}", usuario.id, e.message)
            }
        }

        return enviados
    }

    /** Dispara o resumo de ontem para um usuário específico — usado no botão de teste. */
    @Transactional(readOnly = true)
    fun enviarResumoAgora(usuarioId: UUID): Boolean {
        if (!clienteEmail.habilitado) return false

        val preferencia = preferenciaRepositorio.findByUsuarioGuardianId(usuarioId) ?: return false
        val usuario = preferencia.usuarioGuardian
        val dia = LocalDate.now(FUSO).minusDays(1)

        val inicio = dia.atStartOfDay(FUSO).toInstant()
        val fim    = dia.plusDays(1).atStartOfDay(FUSO).toInstant()

        val alertas = alertaRepositorio
            .findAllByUsuarioGuardianIdAndCriadoEmBetweenOrderByCriadoEmDesc(usuario.id, inicio, fim)
            .filter { it.tipo != TipoAlerta.RESUMO_DIARIO }

        return clienteEmail.enviarHtml(
            destinatario = usuario.email,
            assunto      = "[Guardian] Resumo diário",
            corpoHtml    = modelosEmail.resumoDiario(dia, alertas)
        )
    }
}
