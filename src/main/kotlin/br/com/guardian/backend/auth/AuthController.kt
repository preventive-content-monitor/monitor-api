package br.com.guardian.backend.auth

import br.com.guardian.backend.api.dto.AuthResponse
import br.com.guardian.backend.api.dto.ErrorResponse
import br.com.guardian.backend.api.dto.LoginRequest
import br.com.guardian.backend.api.dto.RegisterRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro de usuários")
class AuthController(
    private val authenticationService: AuthenticationService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Registrar novo usuário",
        description = "Cria uma nova conta de responsável no sistema Guardian"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Usuário criado com sucesso"
            ),
            ApiResponse(
                responseCode = "409",
                description = "Email já cadastrado",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Dados inválidos",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun register(@RequestBody request: RegisterRequest) {
        authenticationService.register(request)
    }

    @PostMapping("/login")
    @Operation(
        summary = "Realizar login",
        description = "Autentica o usuário e retorna um token JWT válido por 24 horas"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login realizado com sucesso",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Credenciais inválidas",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun login(@RequestBody request: LoginRequest): AuthResponse {
        return authenticationService.login(request)
    }
}