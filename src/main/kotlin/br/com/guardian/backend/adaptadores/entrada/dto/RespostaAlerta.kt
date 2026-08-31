package br.com.guardian.backend.adaptadores.entrada.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.*

@Schema(description = "Alerta exibido na Central de Alertas")
data class RespostaAlerta(

    @Schema(description = "ID do alerta")
    val id: UUID,

    @Schema(description = "Categoria do alerta", example = "TENTATIVA_BLOQUEIO")
    val tipo: String,

    @Schema(description = "Peso do alerta", example = "CRITICO")
    val severidade: String,

    @Schema(description = "Título curto")
    val titulo: String,

    @Schema(description = "Descrição do que aconteceu")
    val mensagem: String,

    @Schema(description = "Apelido do dependente envolvido, quando houver")
    val dependente: String? = null,

    @Schema(description = "Domínio ou conteúdo que originou o alerta")
    val referencia: String? = null,

    @Schema(description = "Pontuação de risco de 0 a 100")
    val pontuacaoRisco: Int? = null,

    @Schema(description = "Se o responsável já visualizou")
    val lido: Boolean,

    @Schema(description = "Se o email correspondente foi enviado")
    val emailEnviado: Boolean,

    @Schema(description = "Quando o alerta foi gerado")
    val criadoEm: Instant
)

@Schema(description = "Página de alertas")
data class RespostaPaginaAlertas(
    val itens: List<RespostaAlerta>,
    val pagina: Int,
    val tamanho: Int,
    val totalItens: Long,
    val totalPaginas: Int,

    @Schema(description = "Quantidade de alertas ainda não lidos")
    val naoLidos: Long
)

@Schema(description = "Preferências de notificação por email do responsável")
data class RespostaPreferenciasNotificacao(

    @Schema(description = "Notificar quando um site bloqueado for acessado")
    val tentativasBloqueio: Boolean,

    @Schema(description = "Notificar quando conteúdo de risco for detectado")
    val conteudoSensivel: Boolean,

    @Schema(description = "Receber um resumo por email todo dia às 8h")
    val resumoDiario: Boolean,

    @Schema(description = "Email que receberá as notificações")
    val emailDestino: String,

    @Schema(
        description = "Se o servidor tem SMTP configurado. Quando falso, os alertas " +
            "continuam aparecendo no painel mas nenhum email é enviado."
    )
    val emailHabilitado: Boolean
)

@Schema(description = "Atualização das preferências de notificação")
data class RequisicaoPreferenciasNotificacao(
    val tentativasBloqueio: Boolean = true,
    val conteudoSensivel: Boolean = true,
    val resumoDiario: Boolean = false
)
