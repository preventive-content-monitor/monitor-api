package br.com.guardian.backend.domain.dependent

import br.com.guardian.backend.api.dto.RequisicaoCriarDependente
import br.com.guardian.backend.api.dto.RespostaErro
import br.com.guardian.backend.domain.user.UsuarioRepositorio
import br.com.guardian.backend.exception.DependenteNaoEncontradoExcecao
import br.com.guardian.backend.exception.UsuarioNaoEncontradoExcecao
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
@RequestMapping("/api/dependentes")
@Tag(name = "Dependentes", description = "Gerenciamento de dependentes (crianças/adolescentes)")
@SecurityRequirement(name = "bearerAuth")
class DependenteController(
    private val dependenteRepositorio: DependenteRepositorio,
    private val usuarioRepositorio: UsuarioRepositorio
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Criar dependente",
        description = "Cadastra um novo dependente vinculado ao responsável autenticado"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Dependente criado com sucesso",
                content = [Content(schema = Schema(implementation = Dependente::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Usuário não encontrado",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun criar(
        @RequestBody requisicao: RequisicaoCriarDependente,
        @AuthenticationPrincipal principal: org.springframework.security.oauth2.jwt.Jwt
    ): Dependente {

        val usuarioId = UUID.fromString(principal.subject)

        val usuario = usuarioRepositorio.findById(usuarioId)
            .orElseThrow { UsuarioNaoEncontradoExcecao() }

        val dependente = Dependente(
            usuarioGuardian = usuario,
            apelido = requisicao.apelido,
            anoNascimento = requisicao.anoNascimento
        )

        return dependenteRepositorio.save(dependente)
    }

    @GetMapping
    @Operation(
        summary = "Listar dependentes",
        description = "Retorna todos os dependentes vinculados ao responsável autenticado"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Lista de dependentes retornada com sucesso",
                content = [Content(array = ArraySchema(schema = Schema(implementation = Dependente::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun listar(
        @AuthenticationPrincipal principal: org.springframework.security.oauth2.jwt.Jwt
    ): List<Dependente> {

        val usuarioId = UUID.fromString(principal.subject)

        return dependenteRepositorio.findAllByUsuarioGuardianId(usuarioId)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obter dependente",
        description = "Retorna os detalhes de um dependente específico do responsável autenticado"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Dependente encontrado",
                content = [Content(schema = Schema(implementation = Dependente::class))]
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
    fun buscarPorId(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: org.springframework.security.oauth2.jwt.Jwt
    ): Dependente {
        val usuarioId = UUID.fromString(principal.subject)
        val dependente = dependenteRepositorio.findById(id)
            .orElseThrow { DependenteNaoEncontradoExcecao() }

        if (dependente.usuarioGuardian.id != usuarioId) {
            throw DependenteNaoEncontradoExcecao()
        }

        return dependente
    }
}