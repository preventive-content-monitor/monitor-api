package br.com.guardian.backend.aplicacao.servico

import br.com.guardian.backend.adaptadores.entrada.dto.RespostaAtividade
import br.com.guardian.backend.adaptadores.entrada.dto.RespostaPaginaAtividades
import br.com.guardian.backend.adaptadores.entrada.dto.ResumoAtividades
import br.com.guardian.backend.adaptadores.saida.persistencia.DispositivoRepositorio
import br.com.guardian.backend.adaptadores.saida.persistencia.EventoRepositorio
import br.com.guardian.backend.aplicacao.porta.entrada.ServicoPolitica
import br.com.guardian.backend.dominio.excecao.DispositivoNaoEncontradoExcecao
import br.com.guardian.backend.dominio.modelo.Evento
import br.com.guardian.backend.dominio.modelo.ResultadoClassificacao
import br.com.guardian.backend.dominio.modelo.TipoEvento
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Monta o histórico detalhado de atividades para a tela de Atividades do painel.
 *
 * Diferente do dashboard — que agrega por domínio — aqui cada acesso aparece
 * individualmente com a chave de conteúdo granular. É isso que permite ver
 * QUAL vídeo do YouTube foi acessado, e não apenas que "www.youtube.com" foi visitado.
 */
@Service
class ServicoAtividade(
    private val eventoRepositorio: EventoRepositorio,
    private val dispositivoRepositorio: DispositivoRepositorio,
    private val servicoPolitica: ServicoPolitica
) {

    companion object {
        private const val TAMANHO_PAGINA_MAX = 200
    }

    /**
     * @param usuarioId dono da sessão — o dispositivo precisa pertencer a um
     *        dependente deste usuário, caso contrário o acesso é negado.
     */
    @Transactional(readOnly = true)
    fun buscarHistorico(
        usuarioId: UUID,
        dispositivoId: UUID,
        from: Instant,
        to: Instant,
        pagina: Int,
        tamanho: Int,
        busca: String?,
        rotulo: String?,
        tipo: TipoEvento?,
        riscoMinimo: Int?
    ): RespostaPaginaAtividades {

        validarPropriedade(usuarioId, dispositivoId)

        val buscaLike = busca
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
            ?.let { "%$it%" }

        val rotuloFiltro = rotulo?.trim()?.takeIf { it.isNotBlank() }?.uppercase()

        val paginaSegura  = pagina.coerceAtLeast(0)
        val tamanhoSeguro = tamanho.coerceIn(1, TAMANHO_PAGINA_MAX)

        val resultado = eventoRepositorio.buscarAtividades(
            dispositivoId = dispositivoId,
            from          = from,
            to            = to,
            busca         = buscaLike,
            rotulo        = rotuloFiltro,
            tipo          = tipo,
            riscoMinimo   = riscoMinimo,
            pageable      = PageRequest.of(paginaSegura, tamanhoSeguro)
        )

        // A política é a mesma para todos os itens do dispositivo — busca uma vez só.
        val politica = servicoPolitica.buscarPoliticaPorDispositivo(dispositivoId)

        val itens = resultado.content.map { linha ->
            val evento         = linha[0] as Evento
            val classificacao  = linha.getOrNull(1) as? ResultadoClassificacao
            montarItem(evento, classificacao, politica.limiteRisco, politica.modo.name)
        }

        val resumo = montarResumo(
            dispositivoId, from, to, buscaLike, rotuloFiltro, tipo, riscoMinimo
        )

        return RespostaPaginaAtividades(
            itens        = itens,
            pagina       = resultado.number,
            tamanho      = resultado.size,
            totalItens   = resultado.totalElements,
            totalPaginas = resultado.totalPages,
            resumo       = resumo
        )
    }

    @Transactional(readOnly = true)
    fun rotulosDisponiveis(usuarioId: UUID, dispositivoId: UUID): List<String> {
        validarPropriedade(usuarioId, dispositivoId)
        return eventoRepositorio.rotulosDisponiveis(dispositivoId)
    }

    // ---------------------------------------------------------------

    private fun validarPropriedade(usuarioId: UUID, dispositivoId: UUID) {
        val dispositivo = dispositivoRepositorio.findById(dispositivoId)
            .orElseThrow { DispositivoNaoEncontradoExcecao() }

        // Mesma exceção de "não encontrado" para não revelar a existência de
        // dispositivos de outros usuários.
        if (dispositivo.dependente.usuarioGuardian.id != usuarioId) {
            throw DispositivoNaoEncontradoExcecao()
        }
    }

    private fun montarItem(
        evento: Evento,
        classificacao: ResultadoClassificacao?,
        limiteRisco: Int,
        modoPolitica: String
    ): RespostaAtividade {

        val conteudoKey = classificacao?.urlConteudo
            ?: ServicoIngestaoEvento.extrairConteudoKey(evento.urlHost, "https://${evento.urlHost}")

        val plataformaMista = conteudoKey != evento.urlHost &&
            conteudoKey.removePrefix("www.") != evento.urlHost.removePrefix("www.")

        return RespostaAtividade(
            id              = evento.id,
            tipo            = evento.tipo.name,
            urlHost         = evento.urlHost,
            urlConteudo     = conteudoKey,
            titulo          = evento.titulo,
            ocorridoEm      = evento.ocorridoEm,
            rotulo          = classificacao?.rotulo,
            pontuacaoRisco  = classificacao?.pontuacaoRisco,
            justificativa   = classificacao?.justificativa,
            modelo          = classificacao?.modelo,
            acao            = resolverAcao(evento, classificacao, limiteRisco, modoPolitica),
            plataformaMista = plataformaMista
        )
    }

    /**
     * Reconstrói qual ação a política aplicou a este acesso.
     *
     * Um evento BLOCK_ATTEMPT já é, por definição, um bloqueio que aconteceu.
     * Para os demais, aplica a mesma regra do BlocklistController: rótulo
     * EXPLICIT ou score acima do limite da política resultam no modo da política.
     */
    private fun resolverAcao(
        evento: Evento,
        classificacao: ResultadoClassificacao?,
        limiteRisco: Int,
        modoPolitica: String
    ): String {
        if (evento.tipo == TipoEvento.BLOCK_ATTEMPT) return "BLOCK"
        if (classificacao == null) return "UNKNOWN"
        if (classificacao.rotulo == "SAFE") return "ALLOW"

        val perigoso = classificacao.rotulo == "EXPLICIT" ||
            classificacao.pontuacaoRisco >= limiteRisco

        return if (perigoso) modoPolitica else "ALLOW"
    }

    private fun montarResumo(
        dispositivoId: UUID,
        from: Instant,
        to: Instant,
        buscaLike: String?,
        rotulo: String?,
        tipo: TipoEvento?,
        riscoMinimo: Int?
    ): ResumoAtividades {

        val linha = eventoRepositorio.resumirAtividades(
            dispositivoId, from, to, buscaLike, rotulo, tipo, riscoMinimo
        ).firstOrNull()

        // SUM() sobre conjunto vazio devolve null — daí o fallback para 0.
        fun valor(indice: Int): Long =
            (linha?.getOrNull(indice) as? Number)?.toLong() ?: 0L

        return ResumoAtividades(
            totalAcessos       = valor(0),
            acessosRisco       = valor(1),
            tentativasBloqueio = valor(2),
            dominiosDistintos  = valor(3)
        )
    }
}
