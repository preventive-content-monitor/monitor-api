package br.com.guardian.backend.domain.classification

import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class ClassifierClient {

    fun classify(title: String?, host: String): Pair<String, Int> {

        val text = (title ?: "") + " " + host.lowercase()

        return when {
            text.contains("porn") || text.contains("xxx") ->
                "EXPLICIT" to 95

            text.contains("chat") || text.contains("anon") ->
                "GROOMING_RISK" to 75

            text.contains("game") ->
                "SAFE" to 10

            else -> {
                val score = Random.nextInt(5, 40)
                "SAFE" to score
            }
        }
    }
}