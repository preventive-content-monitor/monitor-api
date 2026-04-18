package br.com.guardian.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GuardianAplicacao

fun main(args: Array<String>) {
    runApplication<GuardianAplicacao>(*args)
}