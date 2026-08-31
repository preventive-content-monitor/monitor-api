package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.Evento
import br.com.guardian.backend.dominio.modelo.TipoEvento
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface EventoRepositorio : JpaRepository<Evento, UUID> {

    fun findAllByDispositivoIdAndOcorridoEmBetween(
        dispositivoId: UUID,
        from: Instant,
        to: Instant
    ): List<Evento>

    @Query(
        """
        select e.urlHost as host, count(e) as cnt
        from Evento e
        where e.dispositivo.id = :dispositivoId
          and e.ocorridoEm between :from and :to
        group by e.urlHost
        order by cnt desc
    """
    )
    fun topHosts(dispositivoId: UUID, from: Instant, to: Instant): List<Array<Any>>

    /**
     * Histórico de atividades paginado: evento + classificação da IA.
     *
     * O join com ResultadoClassificacao é LEFT porque um evento recém-ingerido
     * pode ainda não ter sido classificado. Cada linha retorna [Evento, ResultadoClassificacao?].
     *
     * Filtros opcionais — quando o parâmetro é null o predicado é neutro:
     *  - [busca]  casa contra urlHost, titulo ou urlConteudo (já deve vir em minúsculas e com %)
     *  - [rotulo] filtra pelo rótulo da IA (SAFE, HORROR, EXPLICIT...)
     *  - [tipo]   filtra pelo tipo do evento
     *  - [riscoMinimo] filtra por pontuação de risco mínima
     */
    @Query(
        value = """
        select e, c
        from Evento e
        left join ResultadoClassificacao c on c.evento = e
        where e.dispositivo.id = :dispositivoId
          and e.ocorridoEm between :from and :to
          and (:rotulo is null or c.rotulo = :rotulo)
          and (:tipo is null or e.tipo = :tipo)
          and (:riscoMinimo is null or c.pontuacaoRisco >= :riscoMinimo)
          and (
                :busca is null
                or lower(e.urlHost) like :busca
                or lower(coalesce(e.titulo, '')) like :busca
                or lower(coalesce(c.urlConteudo, '')) like :busca
              )
        order by e.ocorridoEm desc
    """,
        countQuery = """
        select count(e)
        from Evento e
        left join ResultadoClassificacao c on c.evento = e
        where e.dispositivo.id = :dispositivoId
          and e.ocorridoEm between :from and :to
          and (:rotulo is null or c.rotulo = :rotulo)
          and (:tipo is null or e.tipo = :tipo)
          and (:riscoMinimo is null or c.pontuacaoRisco >= :riscoMinimo)
          and (
                :busca is null
                or lower(e.urlHost) like :busca
                or lower(coalesce(e.titulo, '')) like :busca
                or lower(coalesce(c.urlConteudo, '')) like :busca
              )
    """
    )
    fun buscarAtividades(
        @Param("dispositivoId") dispositivoId: UUID,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        @Param("busca") busca: String?,
        @Param("rotulo") rotulo: String?,
        @Param("tipo") tipo: TipoEvento?,
        @Param("riscoMinimo") riscoMinimo: Int?,
        pageable: Pageable
    ): Page<Array<Any>>

    /**
     * Agregados do conjunto filtrado inteiro — usado para os cards de resumo da
     * tela de Atividades, que devem refletir todos os resultados e não só a página atual.
     *
     * Retorna uma única linha: [totalAcessos, acessosRisco, tentativasBloqueio, dominiosDistintos].
     */
    @Query(
        """
        select count(e),
               sum(case when c.pontuacaoRisco >= 70 then 1 else 0 end),
               sum(case when e.tipo = br.com.guardian.backend.dominio.modelo.TipoEvento.BLOCK_ATTEMPT then 1 else 0 end),
               count(distinct e.urlHost)
        from Evento e
        left join ResultadoClassificacao c on c.evento = e
        where e.dispositivo.id = :dispositivoId
          and e.ocorridoEm between :from and :to
          and (:rotulo is null or c.rotulo = :rotulo)
          and (:tipo is null or e.tipo = :tipo)
          and (:riscoMinimo is null or c.pontuacaoRisco >= :riscoMinimo)
          and (
                :busca is null
                or lower(e.urlHost) like :busca
                or lower(coalesce(e.titulo, '')) like :busca
                or lower(coalesce(c.urlConteudo, '')) like :busca
              )
    """
    )
    fun resumirAtividades(
        @Param("dispositivoId") dispositivoId: UUID,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        @Param("busca") busca: String?,
        @Param("rotulo") rotulo: String?,
        @Param("tipo") tipo: TipoEvento?,
        @Param("riscoMinimo") riscoMinimo: Int?
    ): List<Array<Any?>>

    /** Rótulos distintos já registrados para o dispositivo — alimenta o filtro da UI. */
    @Query(
        """
        select distinct c.rotulo
        from Evento e
        join ResultadoClassificacao c on c.evento = e
        where e.dispositivo.id = :dispositivoId
          and c.rotulo is not null
        order by c.rotulo
    """
    )
    fun rotulosDisponiveis(@Param("dispositivoId") dispositivoId: UUID): List<String>
}
