package br.com.guardian.backend.infraestrutura.excecao

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaErro
import br.com.guardian.backend.dominio.excecao.*
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class TratadorGlobalExcecao {

    private val log = LoggerFactory.getLogger(TratadorGlobalExcecao::class.java)

    @ExceptionHandler(RecursoNaoEncontradoExcecao::class)
    fun tratarRecursoNaoEncontrado(
        ex: RecursoNaoEncontradoExcecao,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Recurso não encontrado: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                RespostaErro(
                    status = 404,
                    erro = "Not Found",
                    mensagem = ex.message,
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(RequisicaoInvalidaExcecao::class)
    fun tratarRequisicaoInvalida(
        ex: RequisicaoInvalidaExcecao,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Requisição inválida: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RespostaErro(
                    status = 400,
                    erro = "Bad Request",
                    mensagem = ex.message,
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(NaoAutorizadoExcecao::class, CredenciaisInvalidasExcecao::class)
    fun tratarNaoAutorizado(
        ex: GuardianExcecao,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Não autorizado: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                RespostaErro(
                    status = 401,
                    erro = "Unauthorized",
                    mensagem = ex.message,
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(ConflitoExcecao::class, EmailJaExisteExcecao::class)
    fun tratarConflito(
        ex: GuardianExcecao,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Conflito: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                RespostaErro(
                    status = 409,
                    erro = "Conflict",
                    mensagem = ex.message,
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(
        DispositivoNaoEncontradoExcecao::class,
        DependenteNaoEncontradoExcecao::class,
        UsuarioNaoEncontradoExcecao::class,
        PoliticaNaoEncontradaExcecao::class
    )
    fun tratarDominioNaoEncontrado(
        ex: GuardianExcecao,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Recurso de domínio não encontrado: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                RespostaErro(
                    status = 404,
                    erro = "Not Found",
                    mensagem = ex.message,
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(CodigoVinculacaoInvalidoExcecao::class)
    fun tratarCodigoVinculacaoInvalido(
        ex: CodigoVinculacaoInvalidoExcecao,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Código de vinculação inválido: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RespostaErro(
                    status = 400,
                    erro = "Bad Request",
                    mensagem = ex.message,
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun tratarErrosValidacao(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        val erros = ex.bindingResult.allErrors.map { error ->
            val nomeCampo = (error as? FieldError)?.field ?: "desconhecido"
            val mensagem = error.defaultMessage ?: "Erro de validação"
            "$nomeCampo: $mensagem"
        }

        log.warn("Erros de validação: $erros")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RespostaErro(
                    status = 400,
                    erro = "Validation Error",
                    mensagem = "Erro de validação nos dados enviados",
                    caminho = request.requestURI,
                    detalhes = erros
                )
            )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun tratarParametroAusente(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Parâmetro ausente: ${ex.parameterName}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RespostaErro(
                    status = 400,
                    erro = "Bad Request",
                    mensagem = "Parâmetro obrigatório ausente: ${ex.parameterName}",
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun tratarTipoIncompativel(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Tipo incompatível: ${ex.name} = ${ex.value}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RespostaErro(
                    status = 400,
                    erro = "Bad Request",
                    mensagem = "Parâmetro '${ex.name}' com valor inválido: ${ex.value}",
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun tratarMensagemIlegivel(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Mensagem ilegível: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RespostaErro(
                    status = 400,
                    erro = "Bad Request",
                    mensagem = "Corpo da requisição inválido ou mal formatado",
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun tratarAcessoNegado(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Acesso negado: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                RespostaErro(
                    status = 403,
                    erro = "Forbidden",
                    mensagem = "Acesso negado. Você não tem permissão para acessar este recurso.",
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(AuthenticationException::class)
    fun tratarExcecaoAutenticacao(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.warn("Falha na autenticação: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                RespostaErro(
                    status = 401,
                    erro = "Unauthorized",
                    mensagem = "Autenticação necessária",
                    caminho = request.requestURI
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun tratarExcecaoGenerica(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<RespostaErro> {
        log.error("Erro inesperado", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                RespostaErro(
                    status = 500,
                    erro = "Internal Server Error",
                    mensagem = "Erro interno do servidor. Por favor, tente novamente mais tarde.",
                    caminho = request.requestURI
                )
            )
    }
}
