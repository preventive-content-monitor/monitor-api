package br.com.guardian.backend.adaptadores.entrada.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.*

/**
 * Um item do histórico de atividades — evento de navegação enriquecido com o
 * resultado da classificação da IA.
 *
 * A distinção importante está entre [urlHost] e [urlConteudo]:
 *  - [urlHost]     é o domínio (ex: "www.youtube.com") — é o que aparece no dashboard
 *  - [urlConteudo] é a chave granular do conteúdo específico
 *                  (ex: "youtube.com/watch?v=VIDEO_ID") — permite saber QUAL vídeo
 *                  foi acessado dentro de uma plataforma mista.
 */
@Schema(description = "Item do histórico de atividades com classificação de risco")
data class RespostaAtividade(

    @Schema(description = "ID do evento")
    val id: UUID,

    @Schema(description = "Tipo do evento", example = "NAVIGATION")
    val tipo: String,

    @Schema(description = "Domínio acessado", example = "www.youtube.com")
    val urlHost: String,

    @Schema(
        description = "Chave do conteúdo específico dentro do domínio. Para plataformas mistas identifica o vídeo/página exata.",
        example = "youtube.com/watch?v=dQw4w9WgXcQ"
    )
    val urlConteudo: String? = null,

    @Schema(description = "Título da página ou vídeo capturado pela extensão")
    val titulo: String? = null,

    @Schema(description = "Momento do acesso")
    val ocorridoEm: Instant,

    @Schema(description = "Rótulo atribuído pela IA", example = "HORROR")
    val rotulo: String? = null,

    @Schema(description = "Pontuação de risco de 0 a 100", example = "72")
    val pontuacaoRisco: Int? = null,

    @Schema(description = "Explicação da IA para a classificação")
    val justificativa: String? = null,

    @Schema(description = "Modelo usado na classificação", example = "gpt-4o-mini")
    val modelo: String? = null,

    @Schema(
        description = "Ação resultante da política do dispositivo para este acesso",
        example = "BLOCK",
        allowableValues = ["ALLOW", "BLOCK", "WARN", "EDUCATE", "UNKNOWN"]
    )
    val acao: String,

    @Schema(description = "Indica se o conteúdo é de plataforma mista (YouTube, Twitch...), onde o bloqueio é por conteúdo e não por domínio")
    val plataformaMista: Boolean = false
)

@Schema(description = "Página de resultados do histórico de atividades")
data class RespostaPaginaAtividades(

    @Schema(description = "Itens desta página")
    val itens: List<RespostaAtividade>,

    @Schema(description = "Índice da página atual (base 0)")
    val pagina: Int,

    @Schema(description = "Quantidade de itens por página")
    val tamanho: Int,

    @Schema(description = "Total de itens que atendem aos filtros")
    val totalItens: Long,

    @Schema(description = "Total de páginas disponíveis")
    val totalPaginas: Int,

    @Schema(description = "Agregados do conjunto filtrado inteiro (não apenas da página atual)")
    val resumo: ResumoAtividades
)

@Schema(description = "Agregados do histórico filtrado")
data class ResumoAtividades(

    @Schema(description = "Total de acessos no filtro")
    val totalAcessos: Long,

    @Schema(description = "Acessos com pontuação de risco >= 70")
    val acessosRisco: Long,

    @Schema(description = "Eventos do tipo BLOCK_ATTEMPT")
    val tentativasBloqueio: Long,

    @Schema(description = "Quantidade de domínios distintos")
    val dominiosDistintos: Long
)
