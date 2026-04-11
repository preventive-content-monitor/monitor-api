package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaDispositivo
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoVinculacao
import br.com.guardian.backend.adaptadores.entrada.dto.RespostaErro
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.aplicacao.servico.ServicoVinculacaoDispositivo
import br.com.guardian.backend.dominio.modelo.Dispositivo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/dispositivos")
@Tag(name = "Dispositivos", description = "Gerenciamento e vinculação de dispositivos dos dependentes")
@SecurityRequirement(name = "bearerAuth")
class DispositivoController(
    private val servicoVinculacao: ServicoVinculacaoDispositivo,
    private val dispositivoRepositorio: DispositivoRepositorio
) {

    @GetMapping
    @Operation(
        summary = "Listar dispositivos",
        description = "Retorna todos os dispositivos vinculados aos dependentes do usuário autenticado"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Lista de dispositivos retornada com sucesso",
                content = [Content(array = ArraySchema(schema = Schema(implementation = RespostaDispositivo::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun listarDispositivos(
        @AuthenticationPrincipal principal: org.springframework.security.oauth2.jwt.Jwt
    ): List<RespostaDispositivo> {
        val usuarioId = UUID.fromString(principal.subject)
        return dispositivoRepositorio.buscarTodosPorUsuarioGuardianId(usuarioId).map { dispositivo ->
            val plataforma = extrairPlataforma(dispositivo.nomeDispositivo)
            RespostaDispositivo(
                id = dispositivo.id,
                nome = dispositivo.nomeDispositivo,
                plataforma = plataforma,
                dependenteId = dispositivo.dependente.id,
                apelidoDependente = dispositivo.dependente.apelido,
                vinculadoEm = dispositivo.vinculadoEm,
                ultimoAcessoEm = dispositivo.ultimoAcessoEm
            )
        }
    }

    private fun extrairPlataforma(nomeDispositivo: String): String {
        return when {
            nomeDispositivo.contains("Windows", ignoreCase = true) -> "Windows"
            nomeDispositivo.contains("Mac", ignoreCase = true) -> "macOS"
            nomeDispositivo.contains("Linux", ignoreCase = true) -> "Linux"
            nomeDispositivo.contains("Android", ignoreCase = true) -> "Android"
            nomeDispositivo.contains("iOS", ignoreCase = true) || nomeDispositivo.contains("iPhone", ignoreCase = true) -> "iOS"
            else -> "Desconhecido"
        }
    }

    @PostMapping("/gerar-codigo/{dependenteId}")
    @Operation(
        summary = "Gerar código de vinculação",
        description = "Gera um código de 6 caracteres válido por 5 minutos para vincular um dispositivo ao dependente"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Código gerado com sucesso"
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dependente não encontrado",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun gerarCodigo(@PathVariable dependenteId: UUID): Map<String, String> {
        val codigo = servicoVinculacao.gerarCodigoVinculacao(dependenteId)
        return mapOf("codigo" to codigo)
    }

    @PostMapping("/vincular")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Vincular dispositivo",
        description = "Vincula um dispositivo ao dependente usando o código de vinculação gerado anteriormente"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Dispositivo vinculado com sucesso",
                content = [Content(schema = Schema(implementation = Dispositivo::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Código inválido ou expirado",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dependente não encontrado",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun vincular(@RequestBody requisicao: RequisicaoVinculacao): Dispositivo {
        return servicoVinculacao.vincularDispositivo(requisicao.codigo, requisicao.nomeDispositivo)
    }
}