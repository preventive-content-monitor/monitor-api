package br.com.guardian.backend.domain.device

import br.com.guardian.backend.domain.dependent.DependentRepository
import br.com.guardian.backend.exception.DependentNotFoundException
import br.com.guardian.backend.exception.InvalidEnrollmentCodeException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class DeviceEnrollmentService(
    private val dependentRepository: DependentRepository,
    private val deviceRepository: DeviceRepository
) {

    private val enrollmentCodes = ConcurrentHashMap<String, Pair<UUID, Instant>>()

    fun generateEnrollmentCode(dependentId: UUID): String {
        val code = UUID.randomUUID().toString().substring(0, 6)
        enrollmentCodes[code] = Pair(dependentId, Instant.now().plusSeconds(300))
        return code
    }

    fun enrollDevice(code: String, deviceName: String): Device {
        val entry = enrollmentCodes[code]
            ?: throw InvalidEnrollmentCodeException("Código de vinculação inválido")

        val (dependentId, expiration) = entry

        if (Instant.now().isAfter(expiration)) {
            enrollmentCodes.remove(code)
            throw InvalidEnrollmentCodeException("Código de vinculação expirado")
        }

        val dependent = dependentRepository.findById(dependentId)
            .orElseThrow { DependentNotFoundException() }

        val device = Device(
            dependent = dependent,
            deviceName = deviceName
        )

        enrollmentCodes.remove(code)

        return deviceRepository.save(device)
    }
}