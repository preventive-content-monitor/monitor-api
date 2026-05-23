package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.saida.persistencia.BlockListRepositorio
import br.com.guardian.backend.dominio.modelo.BlockList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Serviço de acesso à tabela block_list — fonte de verdade para classificações conhecidas.
 *
 * Fluxo:
 *  1. Classificação (OpenAI ou fallback) → [registrar] grava/atualiza no MySQL
 *  2. ServicoExportacaoS3 (a cada 5 min)  → lê do MySQL → grava no S3
 *  3. Extensão                             → lê S3 para DNR offline
 *  4. Fallback de classificação            → [estaNaBlacklist] consulta MySQL (mais rápido que S3)
 */
@Service
class ServicoBlocklist(
    private val blockListRepositorio: BlockListRepositorio
) {
    private val logger = LoggerFactory.getLogger(ServicoBlocklist::class.java)

    companion object {
        const val SCORE_BLOQUEIO = 70
    }

    /**
     * Grava ou atualiza a classificação de um conteúdo.
     * Se já existe, sobrescreve com os dados mais recentes.
     */
    fun registrar(urlConteudo: String, rotulo: String, pontuacaoRisco: Int, modelo: String) {
        try {
            val existente = blockListRepositorio.findByUrlConteudo(urlConteudo)
            if (existente != null) {
                existente.rotulo = rotulo
                existente.pontuacaoRisco = pontuacaoRisco
                existente.modelo = modelo
                existente.atualizadoEm = Instant.now()
                blockListRepositorio.save(existente)
                logger.debug("[BlockList] Atualizado: url={} rotulo={} score={}", urlConteudo, rotulo, pontuacaoRisco)
            } else {
                blockListRepositorio.save(
                    BlockList(
                        urlConteudo = urlConteudo,
                        rotulo = rotulo,
                        pontuacaoRisco = pontuacaoRisco,
                        modelo = modelo
                    )
                )
                logger.debug("[BlockList] Registrado: url={} rotulo={} score={}", urlConteudo, rotulo, pontuacaoRisco)
            }
        } catch (e: Exception) {
            logger.error("[BlockList] Erro ao registrar url={}: {}", urlConteudo, e.message)
        }
    }

    /** Busca a entrada completa pelo conteúdo (usado no endpoint /verificar) */
    fun buscarPorConteudo(urlConteudo: String): BlockList? =
        blockListRepositorio.findByUrlConteudo(urlConteudo)

    /** Retorna true se o conteúdo já foi classificado como perigoso */
    fun estaNaBlacklist(urlConteudo: String): Boolean =
        blockListRepositorio.findByUrlConteudo(urlConteudo)
            ?.let { it.pontuacaoRisco >= SCORE_BLOQUEIO } ?: false

    /** Lista de URLs bloqueadas (para exportação S3) */
    fun buscarTodosBloqueados(minScore: Int = SCORE_BLOQUEIO): List<String> =
        blockListRepositorio.findAllBloqueados(minScore).map { it.urlConteudo }

    /** Lista de URLs seguras (para exportação S3) */
    fun buscarTodosSeguros(): List<String> =
        blockListRepositorio.findAllSeguros().map { it.urlConteudo }
}
