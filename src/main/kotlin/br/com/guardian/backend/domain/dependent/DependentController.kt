package br.com.guardian.backend.domain.dependent

import br.com.guardian.backend.api.dto.CreateDependentRequest
import br.com.guardian.backend.api.dto.ErrorResponse
import br.com.guardian.backend.domain.user.UserRepository
import br.com.guardian.backend.exception.DependentNotFoundException
import br.com.guardian.backend.exception.UserNotFoundException
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
@RequestMapping("/api/dependents")
@Tag(name = "Dependentes", description = "Gerenciamento de dependentes (crianças/adolescentes)")
@SecurityRequirement(name = "bearerAuth")
class DependentController(
    private val dependentRepository: DependentRepository,
    private val userRepository: UserRepository
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
                content = [Content(schema = Schema(implementation = Dependent::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Usuário não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun create(
        @RequestBody request: CreateDependentRequest,
        @AuthenticationPrincipal principal: org.springframework.security.oauth2.jwt.Jwt
    ): Dependent {

        val userId = UUID.fromString(principal.subject)

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        val dependent = Dependent(
            guardianUser = user,
            nickname = request.nickname,
            birthYear = request.birthYear
        )

        return dependentRepository.save(dependent)
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
                content = [Content(array = ArraySchema(schema = Schema(implementation = Dependent::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun list(
        @AuthenticationPrincipal principal: org.springframework.security.oauth2.jwt.Jwt
    ): List<Dependent> {

        val userId = UUID.fromString(principal.subject)

        return dependentRepository.findAllByGuardianUserId(userId)
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
                content = [Content(schema = Schema(implementation = Dependent::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token JWT ausente ou inválido",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Dependente não encontrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun getById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: org.springframework.security.oauth2.jwt.Jwt
    ): Dependent {
        val userId = UUID.fromString(principal.subject)
        val dependent = dependentRepository.findById(id)
            .orElseThrow { DependentNotFoundException() }

        if (dependent.guardianUser.id != userId) {
            throw DependentNotFoundException()
        }

        return dependent
    }
}