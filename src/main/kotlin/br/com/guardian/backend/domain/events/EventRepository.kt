package br.com.guardian.backend.domain.events

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface EventRepository : JpaRepository<Event, UUID> {

    fun findAllByDeviceIdAndOccurredAtBetween(
        deviceId: UUID,
        from: Instant,
        to: Instant
    ): List<Event>

    @Query(
        """
        select e.urlHost as host, count(e) as cnt
        from Event e
        where e.device.id = :deviceId
          and e.occurredAt between :from and :to
        group by e.urlHost
        order by cnt desc
    """
    )
    fun topHosts(deviceId: UUID, from: Instant, to: Instant): List<Array<Any>>
}