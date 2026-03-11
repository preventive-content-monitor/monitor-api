package br.com.guardian.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GuardianApplication

fun main(args: Array<String>) {
    runApplication<GuardianApplication>(*args)
}