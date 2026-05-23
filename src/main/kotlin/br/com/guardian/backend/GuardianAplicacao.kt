package br.com.guardian.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GuardianAplicacao

fun main(args: Array<String>) {
    runApplication<GuardianAplicacao>(*args)
}