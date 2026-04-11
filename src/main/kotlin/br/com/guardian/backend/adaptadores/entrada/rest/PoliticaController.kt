package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaErro
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoAtualizarPolitica
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoPolitica
import br.com.guardian.backend.dominio.modelo.Politica
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/politica")
@Tag(name = "Políticas", description = "Gerenciamento de políticas de controle parental")
@SecurityRequirement(name = "bearerAuth")
class PoliticaController(
    private val servicoPolitica: ServicoPolitica
) {

    @GetMapping("/atual")
    @Operation(
        summary = "Obter política atual",
        description = "Retorna a política de controle parental ativa para o dispositivo. Se não existir, cria uma política padrão baseada na idade do dependente."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Política retornada com sucesso",
                content = [Content(schema = Schema(implementation = Politica::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dispositivo não encontrado",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun buscarAtual(
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam dispositivoId: UUID
    ): Politica {
        return servicoPolitica.buscarPoliticaPorDispositivo(dispositivoId)
    }

    @PutMapping
    @Operation(
        summary = "Atualizar política",
        description = "Atualiza a política de controle parental do dispositivo. Permite alterar modo, limite de risco, lista de domínios bloqueados/permitidos e configurações de modo escola."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Política atualizada com sucesso",
                content = [Content(schema = Schema(implementation = Politica::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Dados inválidos na requisição",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dispositivo ou política não encontrada",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun atualizarPolitica(
        @Parameter(description = "ID do dispositivo", required = true)
        @RequestParam dispositivoId: UUID,
        @RequestBody requisicao: RequisicaoAtualizarPolitica
    ): Politica {
        return servicoPolitica.atualizarPolitica(
            dispositivoId = dispositivoId,
            modo = requisicao.modo,
            limiteRisco = requisicao.limiteRisco,
            dominiosBloqueados = requisicao.dominiosBloqueados,
            dominiosPermitidos = requisicao.dominiosPermitidos,
            modoEscolaAtivo = requisicao.modoEscolaAtivo,
            escolaInicio = requisicao.escolaInicio,
            escolaFim = requisicao.escolaFim
        )
    }
}