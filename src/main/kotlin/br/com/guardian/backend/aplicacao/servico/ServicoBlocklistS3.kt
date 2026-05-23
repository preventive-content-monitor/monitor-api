package br.com.guardian.backend.aplicacao.servico

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Service
class ServicoBlocklistS3(
    private val objectMapper: ObjectMapper,
    @Value("\${guardian.s3.bucket:}") private val bucket: String,
    @Value("\${guardian.s3.region:us-east-1}") private val region: String
) {
    private val logger = LoggerFactory.getLogger(ServicoBlocklistS3::class.java)

    companion object {
        // Plataformas com conteúdo misto — nunca devem entrar nas listas globais do S3.
        // Sincronizado com ServicoIngestaoEvento.PLATAFORMAS_MISTAS.
        val PLATAFORMAS_MISTAS = setOf(
            "youtube.com", "www.youtube.com", "youtu.be",
            "twitch.tv", "www.twitch.tv",
            "reddit.com", "www.reddit.com",
            "twitter.com", "www.twitter.com", "x.com", "www.x.com",
            "tiktok.com", "www.tiktok.com",
            "instagram.com", "www.instagram.com",
            "facebook.com", "www.facebook.com",
            "dailymotion.com", "www.dailymotion.com",
            "vimeo.com", "www.vimeo.com",
            "linkedin.com", "www.linkedin.com",
            "pinterest.com", "www.pinterest.com",
        )
    }

    // Lazy init: so cria o cliente se o bucket estiver configurado
    private val s3: S3Client? by lazy {
        if (bucket.isBlank()) {
            logger.warn("[S3] GUARDIAN_S3_BUCKET nao configurado — operacoes S3 desabilitadas.")
            null
        } else {
            try {
                S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build()
                    .also { logger.info("[S3] Cliente inicializado — bucket={} region={}", bucket, region) }
            } catch (e: Exception) {
                logger.error("[S3] Falha ao criar S3Client: {}", e.message)
                null
            }
        }
    }

    fun adicionarAoWhitelist(host: String) = adicionarAoArquivo("whiteList.json", host)

    fun adicionarAoBlacklist(host: String) = adicionarAoArquivo("blackList.json", host)

    /**
     * Sobrescreve o arquivo no S3 com a lista completa fornecida.
     * Chamado pelo ServicoExportacaoS3 a cada 5 minutos.
     */
    fun gravarListaCompleta(chave: String, lista: List<String>) {
        val client = s3 ?: return
        try {
            gravarLista(client, chave, lista)
            logger.info("[S3] {} atualizado com {} entradas", chave, lista.size)
        } catch (e: Exception) {
            logger.error("[S3] Erro ao gravar lista completa em {}: {}", chave, e.message)
        }
    }

    fun estaNaBlacklist(host: String): Boolean {
        val client = s3 ?: return false
        return try {
            lerLista(client, "blackList.json").contains(host)
        } catch (e: Exception) {
            logger.warn("[S3] Falha ao verificar blacklist para host={}: {}", host, e.message)
            false
        }
    }

    fun estaNaWhitelist(host: String): Boolean {
        val client = s3 ?: return false
        return try {
            lerLista(client, "whiteList.json").contains(host)
        } catch (e: Exception) {
            logger.warn("[S3] Falha ao verificar whitelist para host={}: {}", host, e.message)
            false
        }
    }

    private fun adicionarAoArquivo(chave: String, host: String) {
        val client = s3 ?: return
        try {
            val lista = lerLista(client, chave).toMutableList()
            if (lista.contains(host)) return
            lista.add(host)
            gravarLista(client, chave, lista)
            logger.info("[S3] {} adicionado a {} ({} entradas)", host, chave, lista.size)
        } catch (e: Exception) {
            logger.error("[S3] Erro ao atualizar {}: {}", chave, e.message)
        }
    }

    private fun lerLista(client: S3Client, chave: String): List<String> {
        return try {
            val bytes = client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(chave).build()
            )
            objectMapper.readValue<List<String>>(bytes.asUtf8String())
        } catch (e: NoSuchKeyException) {
            emptyList()
        }
    }

    private fun gravarLista(client: S3Client, chave: String, lista: List<String>) {
        val json = objectMapper.writeValueAsString(lista)
        client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(chave)
                .contentType("application/json")
                .build(),
            RequestBody.fromString(json)
        )
    }
}
