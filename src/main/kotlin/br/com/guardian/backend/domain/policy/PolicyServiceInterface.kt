package br.com.guardian.backend.domain.policy

import java.util.*

interface PolicyService {
    fun createDefaultPolicyIfNotExists(dependentId: UUID)
    fun getPolicyByDevice(deviceId: UUID): Policy
    fun updatePolicy(deviceId: UUID, mode: PolicyMode, riskThreshold: Int, blockedDomains: List<String>, allowedDomains: List<String>, schoolModeEnabled: Boolean, schoolStart: String?, schoolEnd: String?): Policy
    fun shouldBlock(domain: String, riskScore: Int, deviceId: UUID): Boolean
}
