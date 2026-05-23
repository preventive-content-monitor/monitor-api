package br.com.guardian.backend.adaptadores.saida.classificacao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class ClienteOpenAi(
    private val objectMapper: ObjectMapper,
    @Value("\${openai.api-key}") private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(ClienteOpenAi::class.java)

    private val restClient = RestClient.builder()
        .baseUrl("https://api.openai.com")
        .build()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RespostaClassificacao(
        val rotulo: String,
        val pontuacaoRisco: Int,
        val justificativa: String
    )

    fun classificar(titulo: String?, urlCompleta: String, host: String): RespostaClassificacao {
        val mensagemUsuario = buildString {
            append("URL: $urlCompleta\n")
            append("Host: $host\n")
            if (!titulo.isNullOrBlank()) append("Título: $titulo\n")
        }

        val corpoRequisicao = mapOf(
            "model" to "gpt-4o-mini",
            "temperature" to 0,
            "response_format" to mapOf("type" to "json_object"),
            "messages" to listOf(
                mapOf("role" to "system", "content" to SYSTEM_PROMPT),
                mapOf("role" to "user", "content" to mensagemUsuario)
            )
        )

        return try {
            val respostaRaw = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(corpoRequisicao))
                .retrieve()
                .body(String::class.java)!!

            val json = objectMapper.readTree(respostaRaw)
            val conteudo = json["choices"][0]["message"]["content"].asText()
            val resultado = objectMapper.readValue(conteudo, RespostaClassificacao::class.java)

            logger.info("[OpenAI] host={} rotulo={} score={}", host, resultado.rotulo, resultado.pontuacaoRisco)
            resultado
        } catch (ex: Exception) {
            logger.error("[OpenAI] Falha ao classificar host={}: {}", host, ex.message)
            throw ex
        }
    }

    companion object {
        private val SYSTEM_PROMPT = """
            Você é um classificador de conteúdo para controle parental infantil.
            Analise a URL e o título fornecidos e classifique se o conteúdo é apropriado para menores de idade.

            Responda APENAS com JSON válido neste formato exato:
            {
              "rotulo": "SAFE" | "EXPLICIT" | "GROOMING_RISK" | "VIOLENCE" | "DRUGS" | "GAMBLING",
              "pontuacaoRisco": <inteiro de 0 a 100>,
              "justificativa": "<breve explicação em português>"
            }

            Critérios:
            - SAFE (0-30): Conteúdo educativo, entretenimento geral, notícias, jogos apropriados para todas as idades.
            - VIOLENCE (31-60): Conteúdo com violência moderada, jogos de ação, notícias sobre crimes.
            - GROOMING_RISK (60-75): Chats anônimos, fóruns sem moderação, aplicativos de encontro, redes de mensagens sem controle de idade.
            - DRUGS (50-75): Conteúdo sobre drogas, álcool, tabaco, substâncias ilícitas.
            - GAMBLING (60-85): Casas de apostas, cassinos online, jogos de azar com dinheiro real, apostas esportivas, sites de poker.
            - EXPLICIT (76-100): Pornografia, conteúdo sexual explícito, nudez, sites adultos (+18).

            Seja conservador: na dúvida, prefira uma pontuação mais alta para proteger crianças.
        """.trimIndent()
    }
}
