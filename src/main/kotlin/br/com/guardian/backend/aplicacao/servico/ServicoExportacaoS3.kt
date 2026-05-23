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
        val bloqueados = servicoBlocklist.buscarTodosBloqueados()
        val seguros = servicoBlocklist.buscarTodosSeguros()

        logger.info(
            "[ExportacaoS3] Iniciando exportacao: {} bloqueados, {} seguros",
            bloqueados.size, seguros.size
        )

        servicoBlocklistS3.gravarListaCompleta("blackList.json", bloqueados)
        servicoBlocklistS3.gravarListaCompleta("whiteList.json", seguros)

        logger.info("[ExportacaoS3] Exportacao concluida com sucesso")
    }
}
