package br.com.guardian.backend.exception

open class GuardianException(
    override val message: String,
    val errorCode: String? = null
) : RuntimeException(message)

class ResourceNotFoundException(
    message: String,
    errorCode: String? = "RESOURCE_NOT_FOUND"
) : GuardianException(message, errorCode)

class BadRequestException(
    message: String,
    errorCode: String? = "BAD_REQUEST"
) : GuardianException(message, errorCode)

class UnauthorizedException(
    message: String,
    errorCode: String? = "UNAUTHORIZED"
) : GuardianException(message, errorCode)

class ConflictException(
    message: String,
    errorCode: String? = "CONFLICT"
) : GuardianException(message, errorCode)

class InvalidCredentialsException(
    message: String = "Credenciais inválidas",
    errorCode: String? = "INVALID_CREDENTIALS"
) : GuardianException(message, errorCode)

class EmailAlreadyExistsException(
    message: String = "Email já cadastrado",
    errorCode: String? = "EMAIL_ALREADY_EXISTS"
) : GuardianException(message, errorCode)

class DeviceNotFoundException(
    message: String = "Dispositivo não encontrado",
    errorCode: String? = "DEVICE_NOT_FOUND"
) : GuardianException(message, errorCode)

class DependentNotFoundException(
    message: String = "Dependente não encontrado",
    errorCode: String? = "DEPENDENT_NOT_FOUND"
) : GuardianException(message, errorCode)

class UserNotFoundException(
    message: String = "Usuário não encontrado",
    errorCode: String? = "USER_NOT_FOUND"
) : GuardianException(message, errorCode)

class PolicyNotFoundException(
    message: String = "Política não encontrada",
    errorCode: String? = "POLICY_NOT_FOUND"
) : GuardianException(message, errorCode)

class InvalidEnrollmentCodeException(
    message: String = "Código de vinculação inválido ou expirado",
    errorCode: String? = "INVALID_ENROLLMENT_CODE"
) : GuardianException(message, errorCode)
