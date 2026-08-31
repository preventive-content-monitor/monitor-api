package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaErro
import br.com.guardian.backend.adaptadores.entrada.dto.RespostaPaginaAtividades
import br.com.guardian.backend.aplicacao.servico.ServicoAtividade
import br.com.guardian.backend.aplicacao.servico.ServicoMetricas
import br.com.guardian.backend.aplicacao.servico.ServicoVulnerabilidade
import br.com.guardian.backend.dominio.modelo.TipoEvento
import br.com.guardian.backend.dominio.modelo.VulnerabilidadeDiaria
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.time.*
import java.util.*

@RestController
@RequestMapping("/api/painel")
@Tag(name = "Painel", description = "Métricas e indicadores para o painel do responsável")
@SecurityRequirement(name = "bearerAuth")
class PainelController(
    private val servicoMetricas: ServicoMetricas,
    private val servicoVulnerabilidade: ServicoVulnerabilidade,
    private val servicoAtividade: ServicoAtividade
) {

    @GetMapping("/resumo")
    @Operation(
        summary = "Resumo de métricas",
        description = "Retorna um resumo das métricas de navegação do dispositivo no período especificado, incluindo total de eventos, eventos sensíveis e tentativas de bloqueio."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Resumo retornado com sucesso"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Parâmetros inválidos",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun resumo(
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam dispositivoId: UUID,
        @Parameter(description = "Data/hora de início (ISO 8601)", example = "2026-02-01T00:00:00Z", required = true)
        @RequestParam from: String,
        @Parameter(description = "Data/hora de fim (ISO 8601)", example = "2026-02-22T23:59:59Z", required = true)
        @RequestParam to: String
    ): Map<String, Any> {

        val inicio = Instant.parse(from)
        val fim = Instant.parse(to)

        return servicoMetricas.resumo(dispositivoId, inicio, fim)
    }

    @GetMapping("/top-dominios")
    @Operation(
        summary = "Top domínios acessados",
        description = "Retorna os domínios mais acessados pelo dispositivo no período especificado, ordenados por quantidade de acessos."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Lista de domínios retornada com sucesso"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Parâmetros inválidos",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun topDominios(
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam dispositivoId: UUID,
        @Parameter(description = "Data/hora de início (ISO 8601)", example = "2026-02-01T00:00:00Z", required = true)
        @RequestParam from: String,
        @Parameter(description = "Data/hora de fim (ISO 8601)", example = "2026-02-22T23:59:59Z", required = true)
        @RequestParam to: String
    ) =
        servicoMetricas.topDominios(
            dispositivoId,
            Instant.parse(from),
            Instant.parse(to)
        )

    @GetMapping("/vulnerabilidade")
    @Operation(
        summary = "Histórico de vulnerabilidade",
        description = "Retorna o histórico diário do índice de vulnerabilidade do dependente. O score varia de 0 a 100, sendo baseado em comportamentos como acesso a conteúdo sensível, tentativas de burlar bloqueios e uso noturno."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Histórico retornado com sucesso",
                content = [Content(array = ArraySchema(schema = Schema(implementation = VulnerabilidadeDiaria::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Parâmetros inválidos",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun vulnerabilidade(
        @Parameter(description = "ID do dependente", required = true)
        @RequestParam dependenteId: UUID,
        @Parameter(description = "Data de início (YYYY-MM-DD)", example = "2026-02-01", required = true)
        @RequestParam from: String,
        @Parameter(description = "Data de fim (YYYY-MM-DD)", example = "2026-02-22", required = true)
        @RequestParam to: String
    ) =
        servicoVulnerabilidade.buscarHistorico(
            dependenteId,
            LocalDate.parse(from),
            LocalDate.parse(to)
        )

    @GetMapping("/atividades")
    @Operation(
        summary = "Histórico detalhado de atividades",
        description = "Lista paginada de cada acesso do dispositivo com a classificação de risco da IA. " +
            "Diferente de /top-dominios, que agrega por domínio, aqui cada acesso traz a chave de conteúdo " +
            "granular (urlConteudo) — permitindo identificar qual vídeo específico foi acessado em plataformas " +
            "mistas como o YouTube, e não apenas que o domínio foi visitado."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Histórico retornado com sucesso",
                content = [Content(schema = Schema(implementation = RespostaPaginaAtividades::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dispositivo não encontrado ou não pertence ao usuário autenticado",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun atividades(
        @AuthenticationPrincipal principal: Jwt,
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam dispositivoId: UUID,
        @Parameter(description = "Data/hora de início (ISO 8601)", example = "2026-02-01T00:00:00Z", required = true)
        @RequestParam from: String,
        @Parameter(description = "Data/hora de fim (ISO 8601)", example = "2026-02-22T23:59:59Z", required = true)
        @RequestParam to: String,
        @Parameter(description = "Índice da página (base 0)")
        @RequestParam(defaultValue = "0") pagina: Int,
        @Parameter(description = "Itens por página (máximo 200)")
        @RequestParam(defaultValue = "50") tamanho: Int,
        @Parameter(description = "Busca livre por domínio, título ou conteúdo")
        @RequestParam(required = false) busca: String?,
        @Parameter(description = "Filtra por rótulo da IA", example = "HORROR")
        @RequestParam(required = false) rotulo: String?,
        @Parameter(description = "Filtra por tipo de evento", example = "BLOCK_ATTEMPT")
        @RequestParam(required = false) tipo: TipoEvento?,
        @Parameter(description = "Pontuação de risco mínima (0-100)", example = "70")
        @RequestParam(required = false) riscoMinimo: Int?
    ): RespostaPaginaAtividades =
        servicoAtividade.buscarHistorico(
            usuarioId     = UUID.fromString(principal.subject),
            dispositivoId = dispositivoId,
            from          = Instant.parse(from),
            to            = Instant.parse(to),
            pagina        = pagina,
            tamanho       = tamanho,
            busca         = busca,
            rotulo        = rotulo,
            tipo          = tipo,
            riscoMinimo   = riscoMinimo
        )

    @GetMapping("/atividades/rotulos")
    @Operation(
        summary = "Rótulos disponíveis",
        description = "Rótulos de classificação já registrados para o dispositivo. Alimenta o filtro da tela de Atividades."
    )
    fun rotulosAtividades(
        @AuthenticationPrincipal principal: Jwt,
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam dispositivoId: UUID
    ): List<String> =
        servicoAtividade.rotulosDisponiveis(
            usuarioId     = UUID.fromString(principal.subject),
            dispositivoId = dispositivoId
        )
}