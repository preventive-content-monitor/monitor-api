package br.com.guardian.backend.exception

import br.com.guardian.backend.api.dto.ErrorResponse
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
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        ex: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Resource not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    status = 404,
                    error = "Not Found",
                    message = ex.message,
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(
        ex: BadRequestException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Bad request: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = 400,
                    error = "Bad Request",
                    message = ex.message,
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(UnauthorizedException::class, InvalidCredentialsException::class)
    fun handleUnauthorized(
        ex: GuardianException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Unauthorized: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    status = 401,
                    error = "Unauthorized",
                    message = ex.message,
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(ConflictException::class, EmailAlreadyExistsException::class)
    fun handleConflict(
        ex: GuardianException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Conflict: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    status = 409,
                    error = "Conflict",
                    message = ex.message,
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(
        DeviceNotFoundException::class,
        DependentNotFoundException::class,
        UserNotFoundException::class,
        PolicyNotFoundException::class
    )
    fun handleDomainNotFound(
        ex: GuardianException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Domain resource not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    status = 404,
                    error = "Not Found",
                    message = ex.message,
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(InvalidEnrollmentCodeException::class)
    fun handleInvalidEnrollmentCode(
        ex: InvalidEnrollmentCodeException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Invalid enrollment code: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = 400,
                    error = "Bad Request",
                    message = ex.message,
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.map { error ->
            val fieldName = (error as? FieldError)?.field ?: "unknown"
            val message = error.defaultMessage ?: "Erro de validação"
            "$fieldName: $message"
        }
        
        log.warn("Validation errors: $errors")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = 400,
                    error = "Validation Error",
                    message = "Erro de validação nos dados enviados",
                    path = request.requestURI,
                    details = errors
                )
            )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Missing parameter: ${ex.parameterName}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = 400,
                    error = "Bad Request",
                    message = "Parâmetro obrigatório ausente: ${ex.parameterName}",
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Type mismatch: ${ex.name} = ${ex.value}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = 400,
                    error = "Bad Request",
                    message = "Parâmetro '${ex.name}' com valor inválido: ${ex.value}",
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Message not readable: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = 400,
                    error = "Bad Request",
                    message = "Corpo da requisição inválido ou mal formatado",
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Access denied: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ErrorResponse(
                    status = 403,
                    error = "Forbidden",
                    message = "Acesso negado. Você não tem permissão para acessar este recurso.",
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Authentication failed: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    status = 401,
                    error = "Unauthorized",
                    message = "Autenticação necessária",
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = 500,
                    error = "Internal Server Error",
                    message = "Erro interno do servidor. Por favor, tente novamente mais tarde.",
                    path = request.requestURI
                )
            )
    }
}
