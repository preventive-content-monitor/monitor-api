package br.com.guardian.backend.adaptadores.saida.email

import br.com.guardian.backend.dominio.modelo.Alerta
import br.com.guardian.backend.dominio.modelo.SeveridadeAlerta
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Templates HTML dos emails.
 *
 * São montados como string em vez de Thymeleaf porque clientes de email exigem
 * CSS inline e tabelas — um template engine não ajudaria e traria dependência.
 */
@Component
class ModelosEmail {

    companion object {
        private val FUSO = ZoneId.of("America/Sao_Paulo")
        private val HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(FUSO)
        private val DIA  = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(FUSO)

        private const val AZUL     = "#2563eb"
        private const val CINZA    = "#64748b"
        private const val TEXTO    = "#0f172a"
        private const val BORDA    = "#e2e8f0"
        private const val FUNDO    = "#f8fafc"
    }

    private fun corDaSeveridade(severidade: SeveridadeAlerta) = when (severidade) {
        SeveridadeAlerta.CRITICO  -> "#dc2626"
        SeveridadeAlerta.ATENCAO  -> "#d97706"
        SeveridadeAlerta.INFO     -> AZUL
        SeveridadeAlerta.POSITIVO -> "#059669"
    }

    private fun rotuloDaSeveridade(severidade: SeveridadeAlerta) = when (severidade) {
        SeveridadeAlerta.CRITICO  -> "Crítico"
        SeveridadeAlerta.ATENCAO  -> "Atenção"
        SeveridadeAlerta.INFO     -> "Informativo"
        SeveridadeAlerta.POSITIVO -> "Positivo"
    }

    private fun escapar(texto: String): String = texto
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    /** Moldura comum a todos os emails: cabeçalho, corpo e rodapé. */
    private fun moldura(titulo: String, corAcento: String, conteudo: String): String = """
        <!DOCTYPE html>
        <html lang="pt-br">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="margin:0;padding:0;background:$FUNDO;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;">
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:$FUNDO;padding:24px 12px;">
            <tr><td align="center">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border:1px solid $BORDA;border-radius:12px;overflow:hidden;">

                <tr><td style="background:$corAcento;padding:20px 28px;">
                  <div style="color:#ffffff;font-size:13px;font-weight:600;letter-spacing:0.08em;text-transform:uppercase;opacity:0.85;">Guardian</div>
                  <div style="color:#ffffff;font-size:20px;font-weight:700;margin-top:4px;">${escapar(titulo)}</div>
                </td></tr>

                <tr><td style="padding:28px;color:$TEXTO;font-size:15px;line-height:1.6;">
                  $conteudo
                </td></tr>

                <tr><td style="padding:18px 28px;background:$FUNDO;border-top:1px solid $BORDA;color:$CINZA;font-size:12px;line-height:1.5;">
                  Você recebeu este email porque ativou notificações no painel do Guardian.<br>
                  Para deixar de receber, desmarque a opção correspondente na tela de Alertas.
                </td></tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
    """.trimIndent()

    /** Email de um alerta individual (tentativa de bloqueio ou conteúdo sensível). */
    fun alertaIndividual(alerta: Alerta, apelidoDependente: String?): String {
        val cor = corDaSeveridade(alerta.severidade)

        val linhas = buildString {
            if (apelidoDependente != null) {
                append(linhaDetalhe("Dependente", escapar(apelidoDependente)))
            }
            if (alerta.referencia != null) {
                append(linhaDetalhe("Conteúdo", "<code style=\"font-family:monospace;font-size:13px;\">${escapar(alerta.referencia)}</code>"))
            }
            if (alerta.pontuacaoRisco != null) {
                append(linhaDetalhe("Risco", "<strong style=\"color:$cor;\">${alerta.pontuacaoRisco}/100</strong>"))
            }
            append(linhaDetalhe("Quando", HORA.format(alerta.criadoEm)))
        }

        val conteudo = """
            <div style="display:inline-block;padding:4px 10px;border-radius:999px;background:${cor}18;color:$cor;font-size:12px;font-weight:700;margin-bottom:16px;">
              ${rotuloDaSeveridade(alerta.severidade)}
            </div>

            <p style="margin:0 0 20px;">${escapar(alerta.mensagem)}</p>

            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid $BORDA;border-radius:8px;">
              $linhas
            </table>
        """.trimIndent()

        return moldura(alerta.titulo, cor, conteudo)
    }

    private fun linhaDetalhe(rotulo: String, valor: String): String = """
        <tr>
          <td style="padding:10px 14px;border-bottom:1px solid $BORDA;color:$CINZA;font-size:13px;width:110px;">$rotulo</td>
          <td style="padding:10px 14px;border-bottom:1px solid $BORDA;color:$TEXTO;font-size:14px;">$valor</td>
        </tr>
    """.trimIndent()

    /** Resumo diário consolidado. */
    fun resumoDiario(dia: LocalDate, alertas: List<Alerta>): String {
        val criticos = alertas.count { it.severidade == SeveridadeAlerta.CRITICO }
        val atencao  = alertas.count { it.severidade == SeveridadeAlerta.ATENCAO }

        val corTopo = when {
            criticos > 0 -> "#dc2626"
            atencao > 0  -> "#d97706"
            else         -> "#059669"
        }

        val conteudo = if (alertas.isEmpty()) {
            """
            <p style="margin:0 0 8px;font-size:16px;"><strong>Nenhum incidente ontem.</strong></p>
            <p style="margin:0;color:$CINZA;">
              Não houve tentativas de acesso bloqueado nem detecção de conteúdo sensível
              em ${DIA.format(dia.atStartOfDay(FUSO).toInstant())}.
            </p>
            """.trimIndent()
        } else {
            val cards = """
                <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 20px;">
                  <tr>
                    ${cardNumero(criticos, if (criticos == 1) "crítico" else "críticos", "#dc2626")}
                    ${cardNumero(atencao, "de atenção", "#d97706")}
                    ${cardNumero(alertas.size, "no total", AZUL)}
                  </tr>
                </table>
            """.trimIndent()

            // Limita a lista para não gerar um email gigante em dias muito ativos
            val mostrados = alertas.take(10)
            val itens = mostrados.joinToString("") { alerta ->
                val cor = corDaSeveridade(alerta.severidade)
                """
                <tr>
                  <td style="padding:12px 14px;border-bottom:1px solid $BORDA;">
                    <div style="font-weight:600;color:$TEXTO;font-size:14px;">
                      <span style="color:$cor;">●</span> ${escapar(alerta.titulo)}
                    </div>
                    <div style="color:$CINZA;font-size:13px;margin-top:3px;">${escapar(alerta.mensagem)}</div>
                    <div style="color:$CINZA;font-size:12px;margin-top:3px;">${HORA.format(alerta.criadoEm)}</div>
                  </td>
                </tr>
                """.trimIndent()
            }

            val restante = alertas.size - mostrados.size
            val rodapeLista = if (restante > 0) {
                """<p style="margin:14px 0 0;color:$CINZA;font-size:13px;">
                     E mais $restante ${if (restante == 1) "alerta" else "alertas"} no painel.
                   </p>"""
            } else ""

            """
            $cards
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid $BORDA;border-radius:8px;">
              $itens
            </table>
            $rodapeLista
            """.trimIndent()
        }

        return moldura("Resumo de ${DIA.format(dia.atStartOfDay(FUSO).toInstant())}", corTopo, conteudo)
    }

    private fun cardNumero(valor: Int, rotulo: String, cor: String): String = """
        <td style="padding-right:10px;">
          <div style="border:1px solid $BORDA;border-radius:8px;padding:12px 16px;min-width:76px;">
            <div style="font-size:24px;font-weight:700;color:$cor;line-height:1;">$valor</div>
            <div style="font-size:12px;color:$CINZA;margin-top:4px;">$rotulo</div>
          </div>
        </td>
    """.trimIndent()

    /** Email de teste disparado pelo botão da tela de Alertas. */
    fun emailTeste(emailDestino: String): String {
        val conteudo = """
            <p style="margin:0 0 16px;font-size:16px;"><strong>O envio de emails está funcionando.</strong></p>
            <p style="margin:0 0 20px;color:$CINZA;">
              Este é um email de teste enviado a partir do painel do Guardian.
              Se você recebeu esta mensagem, os alertas que você ativou chegarão neste endereço.
            </p>
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border:1px solid $BORDA;border-radius:8px;">
              ${linhaDetalhe("Destinatário", escapar(emailDestino))}
              ${linhaDetalhe("Enviado em", HORA.format(Instant.now()))}
            </table>
        """.trimIndent()

        return moldura("Teste de notificação", "#059669", conteudo)
    }
}
