package br.com.guardian.backend.adaptadores.entrada.rest

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaAutenticacao
import br.com.guardian.backend.adaptadores.entrada.dto.RespostaErro
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoLogin
import br.com.guardian.backend.adaptadores.entrada.dto.RequisicaoRegistro
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoAutenticacao
import br.com.guardian.backend.aplicacao.servico.ServicoSessao
import br.com.guardian.backend.dominio.modelo.TipoCredencial
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/autenticacao")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro de usuários")
class AutenticacaoController(
    private val servicoAutenticacao: ServicoAutenticacao,
    private val servicoSessao: ServicoSessao
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

    @PostMapping("/entrar-configuracao")
    fun autenticarConfiguracao(@RequestBody requisicao: RequisicaoLogin): RespostaAutenticacao =
        servicoSessao.autenticarConfiguracao(requisicao)

    @PostMapping("/entrar-vinculo")
    fun autenticarVinculacao(@RequestBody requisicao: RequisicaoLogin): RespostaAutenticacao =
        servicoAutenticacao.autenticar(requisicao, TipoCredencial.VINCULACAO)

    @PostMapping("/sessao-menor")
    fun iniciarConfiguracao(@AuthenticationPrincipal principal: Jwt): RespostaAutenticacao =
        servicoSessao.iniciarConfiguracao(principal)

    @PostMapping("/autorizacao-vinculo")
    fun autorizarVinculacao(@AuthenticationPrincipal principal: Jwt): RespostaAutenticacao =
        servicoSessao.autorizarVinculacao(principal)

    @PostMapping("/sair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun encerrar(@AuthenticationPrincipal principal: Jwt) {
        servicoSessao.encerrar(principal)
    }
}