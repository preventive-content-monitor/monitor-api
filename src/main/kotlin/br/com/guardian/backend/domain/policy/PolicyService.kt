package br.com.guardian.backend.domain.policy

import br.com.guardian.backend.domain.dependent.DependentRepository
import br.com.guardian.backend.domain.device.DeviceRepository
import br.com.guardian.backend.exception.DependentNotFoundException
import br.com.guardian.backend.exception.DeviceNotFoundException
import br.com.guardian.backend.exception.PolicyNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.util.*

@Service
class PolicyService(
    private val policyRepository: PolicyRepository,
    private val dependentRepository: DependentRepository,
    private val deviceRepository: DeviceRepository,
    private val objectMapper: ObjectMapper
) {

    fun createDefaultPolicyIfNotExists(dependentId: UUID) {
        if (policyRepository.findByDependentId(dependentId) != null) return

        val dependent = dependentRepository.findById(dependentId)
            .orElseThrow { DependentNotFoundException() }

        val age = java.time.Year.now().value - dependent.birthYear

        val mode = when {
            age <= 10 -> PolicyMode.BLOCK
            age <= 13 -> PolicyMode.WARN
            else -> PolicyMode.EDUCATE
        }

        val threshold = when {
            age <= 10 -> 30
            age <= 13 -> 50
            else -> 70
        }

        val policy = Policy(
            dependent = dependent,
            mode = mode,
            riskThreshold = threshold,
            blockedDomains = objectMapper.writeValueAsString(emptyList<String>())
        )

        policyRepository.save(policy)
    }

    fun getPolicyByDevice(deviceId: UUID): Policy {
        val device = deviceRepository.findById(deviceId)
            .orElseThrow { DeviceNotFoundException() }

        val dependentId = device.dependent.id

        createDefaultPolicyIfNotExists(dependentId)

        return policyRepository.findByDependentId(dependentId)
            ?: throw PolicyNotFoundException()
    }

    fun updatePolicy(deviceId: UUID, mode: PolicyMode, riskThreshold: Int, blockedDomains: List<String>, allowedDomains: List<String>, schoolModeEnabled: Boolean, schoolStart: String?, schoolEnd: String?): Policy {
        val device = deviceRepository.findById(deviceId)
            .orElseThrow { DeviceNotFoundException() }

        val dependentId = device.dependent.id

        val policy = policyRepository.findByDependentId(dependentId)
            ?: throw PolicyNotFoundException()

        policy.mode = mode
        policy.riskThreshold = riskThreshold
        policy.blockedDomains = objectMapper.writeValueAsString(blockedDomains)
        policy.allowedDomains = objectMapper.writeValueAsString(allowedDomains)
        policy.schoolModeEnabled = schoolModeEnabled
        policy.schoolStart = schoolStart
        policy.schoolEnd = schoolEnd

        return policyRepository.save(policy)
    }

    fun shouldBlock(domain: String, riskScore: Int, deviceId: UUID): Boolean {
        val policy = getPolicyByDevice(deviceId)

        val blockedList: List<String> =
            objectMapper.readValue(policy.blockedDomains, List::class.java) as List<String>

        if (blockedList.contains(domain)) return true

        return riskScore >= policy.riskThreshold && policy.mode == PolicyMode.BLOCK
    }
}