package br.com.guardian.backend.api.dto

data class EnrollmentRequest(
    val code: String,
    val deviceName: String
)