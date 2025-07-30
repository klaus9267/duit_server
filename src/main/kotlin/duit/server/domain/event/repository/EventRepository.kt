package duit.server.domain.event.repository

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

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
}