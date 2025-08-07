package duit.server.domain.event.repository

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface EventRepository : JpaRepository<Event, Long> {
    @Query(
        """
        SELECT e
        FROM Event e
        JOIN e.host h
        WHERE isApproved = :isApproved
        AND (:hostId IS NULL OR h.id = :hostId)
        AND (:type IS NULL OR e.eventType = :type)
        """
    )
    fun findWithFilter(type: EventType?, hostId: Long?, isApproved: Boolean, pageable: Pageable): Page<Event>

    @Query(
        """
        SELECT e
        FROM Event e
        WHERE e.isApproved = true
        AND e.startAt BETWEEN :startDate AND :endDate
        AND (:eventType IS NULL OR e.eventType = :eventType)
        ORDER BY e.startAt ASC
        """
    )
    fun findEvents4Calendar(
        startDate: LocalDate,
        endDate: LocalDate,
        eventType: EventType?
    ): List<Event>
}