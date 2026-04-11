package br.com.guardian.backend.domain.classification

import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class ClienteClassificador {

    fun classificar(titulo: String?, host: String): Pair<String, Int> {

        val texto = (titulo ?: "") + " " + host.lowercase()

        return when {
            texto.contains("porn") || texto.contains("xxx") ->
                "EXPLICIT" to 95

            texto.contains("chat") || texto.contains("anon") ->
                "GROOMING_RISK" to 75

            texto.contains("game") ->
                "SAFE" to 10

            else -> {
                val pontuacao = Random.nextInt(5, 40)
                "SAFE" to pontuacao
            }
        }
    }
}