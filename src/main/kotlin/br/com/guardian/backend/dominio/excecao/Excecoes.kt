package br.com.guardian.backend.dominio.excecao

open class GuardianExcecao(
    override val message: String,
    val codigoErro: String? = null
) : RuntimeException(message)

class RecursoNaoEncontradoExcecao(
    message: String,
    codigoErro: String? = "RESOURCE_NOT_FOUND"
) : GuardianExcecao(message, codigoErro)

class RequisicaoInvalidaExcecao(
    message: String,
    codigoErro: String? = "BAD_REQUEST"
) : GuardianExcecao(message, codigoErro)

class NaoAutorizadoExcecao(
    message: String,
    codigoErro: String? = "UNAUTHORIZED"
) : GuardianExcecao(message, codigoErro)

class ConflitoExcecao(
    message: String,
    codigoErro: String? = "CONFLICT"
) : GuardianExcecao(message, codigoErro)

class CredenciaisInvalidasExcecao(
    message: String = "Credenciais inválidas",
    codigoErro: String? = "INVALID_CREDENTIALS"
) : GuardianExcecao(message, codigoErro)

class EmailJaExisteExcecao(
    message: String = "Email já cadastrado",
    codigoErro: String? = "EMAIL_ALREADY_EXISTS"
) : GuardianExcecao(message, codigoErro)

class DispositivoNaoEncontradoExcecao(
    message: String = "Dispositivo não encontrado",
    codigoErro: String? = "DEVICE_NOT_FOUND"
) : GuardianExcecao(message, codigoErro)

class DependenteNaoEncontradoExcecao(
    message: String = "Dependente não encontrado",
    codigoErro: String? = "DEPENDENT_NOT_FOUND"
) : GuardianExcecao(message, codigoErro)

class UsuarioNaoEncontradoExcecao(
    message: String = "Usuário não encontrado",
    codigoErro: String? = "USER_NOT_FOUND"
) : GuardianExcecao(message, codigoErro)

class PoliticaNaoEncontradaExcecao(
    message: String = "Política não encontrada",
    codigoErro: String? = "POLICY_NOT_FOUND"
) : GuardianExcecao(message, codigoErro)

class CodigoVinculacaoInvalidoExcecao(
    message: String = "Código de vinculação inválido ou expirado",
    codigoErro: String? = "INVALID_ENROLLMENT_CODE"
) : GuardianExcecao(message, codigoErro)
