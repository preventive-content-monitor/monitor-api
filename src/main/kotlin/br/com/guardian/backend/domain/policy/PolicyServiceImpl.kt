package br.com.guardian.backend.domain.policy

import br.com.guardian.backend.domain.dependent.DependentRepository
import br.com.guardian.backend.domain.device.DeviceRepository
import br.com.guardian.backend.exception.DependentNotFoundException
import br.com.guardian.backend.exception.DeviceNotFoundException
import br.com.guardian.backend.exception.PolicyNotFoundException
import org.springframework.stereotype.Service
import java.util.*

@Service
class PolicyServiceImpl(
    private val policyRepository: PolicyRepository,
    private val dependentRepository: DependentRepository,
    private val deviceRepository: DeviceRepository,
    private val jsonSerializer: JsonSerializer,
    private val policyCalculationStrategy: PolicyCalculationStrategy,
    private val ageCalculator: AgeCalculator
) : PolicyService {

    override fun createDefaultPolicyIfNotExists(dependentId: UUID) {
        if (policyRepository.findByDependentId(dependentId) != null) return

        val dependent = dependentRepository.findById(dependentId)
            .orElseThrow { DependentNotFoundException() }

        val age = ageCalculator.calculateAge(dependent)
        val config = policyCalculationStrategy.calculatePolicy(age)

        val policy = Policy(
            dependent = dependent,
            mode = config.mode,
            riskThreshold = config.riskThreshold,
            blockedDomains = jsonSerializer.serialize(emptyList<String>())
        )

        policyRepository.save(policy)
    }

    override fun getPolicyByDevice(deviceId: UUID): Policy {
        val device = deviceRepository.findById(deviceId)
            .orElseThrow { DeviceNotFoundException() }

        val dependentId = device.dependent.id

        createDefaultPolicyIfNotExists(dependentId)

        return policyRepository.findByDependentId(dependentId)
            ?: throw PolicyNotFoundException()
    }

    override fun updatePolicy(deviceId: UUID, mode: PolicyMode, riskThreshold: Int, blockedDomains: List<String>, allowedDomains: List<String>, schoolModeEnabled: Boolean, schoolStart: String?, schoolEnd: String?): Policy {
        val device = deviceRepository.findById(deviceId)
            .orElseThrow { DeviceNotFoundException() }

        val dependentId = device.dependent.id

        val policy = policyRepository.findByDependentId(dependentId)
            ?: throw PolicyNotFoundException()

        policy.mode = mode
        policy.riskThreshold = riskThreshold
        policy.blockedDomains = jsonSerializer.serialize(blockedDomains)
        policy.allowedDomains = jsonSerializer.serialize(allowedDomains)
        policy.schoolModeEnabled = schoolModeEnabled
        policy.schoolStart = schoolStart
        policy.schoolEnd = schoolEnd

        return policyRepository.save(policy)
    }

    override fun shouldBlock(domain: String, riskScore: Int, deviceId: UUID): Boolean {
        val policy = getPolicyByDevice(deviceId)

        val blockedList: List<String> = jsonSerializer.deserializeList(policy.blockedDomains, String::class.java)

        if (blockedList.contains(domain)) return true

        return riskScore >= policy.riskThreshold && policy.mode == PolicyMode.BLOCK
    }
}
