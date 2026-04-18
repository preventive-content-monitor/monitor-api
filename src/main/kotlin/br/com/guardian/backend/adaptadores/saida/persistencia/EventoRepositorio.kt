package br.com.guardian.backend.adaptadores.saida.persistencia

import br.com.guardian.backend.dominio.modelo.Evento
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
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
}