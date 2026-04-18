package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaAutenticacao
import br.com.guardian.backend.adaptadores.entrada.dto.RespostaErro
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoLogin
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoRegistro
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoAutenticacao
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/autenticacao")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro de usuários")
class AutenticacaoController(
    private val servicoAutenticacao: ServicoAutenticacao
) {

    @PostMapping("/registrar")
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
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Dados inválidos",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun registrar(@RequestBody requisicao: RequisicaoRegistro) {
        servicoAutenticacao.registrar(requisicao)
    }

    @PostMapping("/entrar")
    @Operation(
        summary = "Realizar login",
        description = "Autentica o usuário e retorna um token JWT válido por 24 horas"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Login realizado com sucesso",
                content = [Content(schema = Schema(implementation = RespostaAutenticacao::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Credenciais inválidas",
                content = [Content(schema = Schema(implementation = RespostaErro::class))]
            )
        ]
    )
    fun autenticar(@RequestBody requisicao: RequisicaoLogin): RespostaAutenticacao {
        return servicoAutenticacao.autenticar(requisicao)
    }
}