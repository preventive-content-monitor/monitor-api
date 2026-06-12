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

    fun classificar(
        titulo: String?,
        urlCompleta: String,
        host: String,
        descricao: String? = null,
        restricaoEtaria: Boolean = false,
        categoria: String? = null
    ): RespostaClassificacao {
        val mensagemUsuario = buildString {
            append("URL: $urlCompleta\n")
            append("Host: $host\n")
            if (!titulo.isNullOrBlank())    append("Título: $titulo\n")
            if (!descricao.isNullOrBlank()) append("Descrição: ${descricao.take(300)}\n")
            if (!categoria.isNullOrBlank()) append("Categoria: $categoria\n")
            if (restricaoEtaria)            append("AVISO: Este conteúdo possui restrição etária explícita (+18)\n")
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
            Você é um classificador de conteúdo para controle parental infantil rigoroso.
            Analise a URL, título, descrição e metadados fornecidos e classifique se o conteúdo é apropriado para crianças e adolescentes menores de 18 anos.

            Responda APENAS com JSON válido neste formato exato:
            {
              "rotulo": "SAFE" | "VIOLENCE" | "HORROR" | "EXPLICIT" | "GROOMING_RISK" | "DRUGS" | "GAMBLING",
              "pontuacaoRisco": <inteiro de 0 a 100>,
              "justificativa": "<breve explicação em português>"
            }

            Critérios de classificação (seja SEMPRE conservador — na dúvida, pontue mais alto):

            - SAFE (0-25): Conteúdo educativo, ciência, natureza, culinária, esportes, música infantil, entretenimento familiar, notícias neutras. Adequado para todas as idades.

            - VIOLENCE (26-55): Jogos de ação com violência leve/moderada, documentários sobre guerras/crimes, notícias sobre violência urbana, lutas/combates esportivos. Não recomendado para crianças pequenas.

            - HORROR (56-80): Terror, suspense, jump scares, fantasmas, demônios, criaturas, pesadelos, compilados assustadores, conteúdo perturbador psicologicamente, títulos com palavras como "terror", "assustador", "pesadelo", "medo extremo", "perturbador". SEMPRE classifique como HORROR se o título contiver linguagem de terror intensa. NÃO É SAFE para menores de 14 anos.

            - DRUGS (50-75): Conteúdo sobre drogas ilícitas, álcool, tabaco, substâncias psicoativas, "como fazer", glamourização de vícios.

            - GROOMING_RISK (60-80): Chats anônimos, apps de encontro, fóruns sem moderação de idade, conteúdo que incentiva contato com estranhos.

            - GAMBLING (60-85): Apostas online, cassinos, loot boxes, jogos de azar com dinheiro real.

            - EXPLICIT (76-100): Pornografia, conteúdo sexual explícito, nudez, sites adultos (+18). Se houver restrição etária explícita, pontuação mínima de 85.

            REGRAS ESPECIAIS:
            1. Se o título contiver "terror", "horror", "assustador", "pesadelo", "medo", "perturbador", "extremo", "jump scare" ou similares em PT/EN → mínimo 60 pontos (HORROR).
            2. Se houver restrição etária (+18) explícita → mínimo 85 pontos (EXPLICIT).
            3. Conteúdo que mistura violência extrema + terror → mínimo 70 pontos.
            4. Títulos em CAPS LOCK com conteúdo de terror são intencionalmente sensacionalistas → trate como mais grave.
            5. Compilados de sustos, vídeos virais de terror, "videos assustadores" → mínimo 60 pontos.
        """.trimIndent()
    }
}
