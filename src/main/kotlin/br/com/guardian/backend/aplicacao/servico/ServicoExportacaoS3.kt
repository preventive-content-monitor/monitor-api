package br.com.guardian.backend.aplicacao.servico

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Exporta periodicamente a tabela block_list (MySQL) para o S3.
 *
 * Estratégia:
 *  - MySQL é a fonte de verdade (escrita rápida, indexada)
 *  - S3 é cache de leitura para a extensão Chrome (offline/DNR)
 *  - A cada 5 minutos o S3 é sincronizado com o estado atual do banco
 */
@Service
class ServicoExportacaoS3(
    private val servicoBlocklist: ServicoBlocklist,
    private val servicoBlocklistS3: ServicoBlocklistS3
) {
    private val logger = LoggerFactory.getLogger(ServicoExportacaoS3::class.java)

    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    fun exportarParaS3() {
        // Exporta APENAS a blacklist (score >= 70) para o S3.
        //
        // A whitelist NÃO é exportada porque:
        //  1. A extensão não precisa dela para o DNR (só a blacklist é usada para bloquear)
        //  2. Exportar a whitelist faria o arquivo crescer indefinidamente (todo site safe vira entrada)
        //  3. Havia um bug: a whitelist S3 na extensão podia remover bloqueios explícitos do responsável
        //
        // A whitelist existe apenas no DB — usada pelo backend para cache de classificação
        // e pelo endpoint GET /api/blocklist/verificar.
        val bloqueados = servicoBlocklist.buscarTodosBloqueados()

        logger.info("[ExportacaoS3] Exportando {} entradas para blackList.json", bloqueados.size)
        servicoBlocklistS3.gravarListaCompleta("blackList.json", bloqueados)
        logger.info("[ExportacaoS3] Exportacao concluida com sucesso")
    }
}
