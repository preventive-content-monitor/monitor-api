package br.com.guardian.backend.adaptadores.saida.email

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

/**
 * Envio de email via SMTP.
 *
 * As credenciais vêm do ambiente (`GUARDIAN_MAIL_USERNAME` / `GUARDIAN_MAIL_PASSWORD`),
 * injetadas no serviço systemd pelo passo 7 da automação de infraestrutura.
 *
 * Quando o SMTP não está configurado o cliente vira no-op: os alertas continuam
 * sendo criados e visíveis no painel, apenas o email não sai. Isso mantém o
 * ambiente local e o lab do AWS Academy funcionando sem credenciais de email.
 */
@Component
class ClienteEmail(
    private val mailSenderProvider: ObjectProvider<JavaMailSender>,
    @Value("\${guardian.mail.remetente:}") private val remetente: String,
    @Value("\${guardian.mail.nome-remetente:Guardian}") private val nomeRemetente: String,
    @Value("\${spring.mail.host:}") private val host: String
) {
    private val logger = LoggerFactory.getLogger(ClienteEmail::class.java)

    /** True quando há host SMTP e remetente configurados. */
    val habilitado: Boolean
        get() = host.isNotBlank() && remetente.isNotBlank()

    /**
     * @return true se o email saiu; false se o SMTP está desligado ou o envio falhou.
     *         Falha de email nunca propaga — não pode derrubar a ingestão de eventos.
     */
    fun enviarHtml(destinatario: String, assunto: String, corpoHtml: String): Boolean {
        if (!habilitado) {
            logger.debug("[Email] SMTP nao configurado — ignorando envio para {}", destinatario)
            return false
        }

        val mailSender = mailSenderProvider.getIfAvailable()
        if (mailSender == null) {
            logger.warn("[Email] JavaMailSender indisponivel — verifique spring.mail.host")
            return false
        }

        return try {
            val mensagem: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mensagem, false, "UTF-8")

            helper.setFrom(remetente, nomeRemetente)
            helper.setTo(destinatario)
            helper.setSubject(assunto)
            helper.setText(corpoHtml, true)

            mailSender.send(mensagem)
            logger.info("[Email] Enviado para {} — assunto: {}", destinatario, assunto)
            true
        } catch (e: Exception) {
            logger.error("[Email] Falha ao enviar para {}: {}", destinatario, e.message)
            false
        }
    }
}
