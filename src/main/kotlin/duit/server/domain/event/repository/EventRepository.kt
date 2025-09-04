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
        SELECT DISTINCT e
        FROM Event e
        JOIN e.host h
        LEFT JOIN e.view v
        LEFT JOIN Bookmark b ON b.event.id = e.id AND b.user.id = :userId
        WHERE e.isApproved = :isApproved
        AND (:hostId IS NULL OR h.id = :hostId)
        AND (:type IS NULL OR e.eventType IN :type)
        AND (:includeFinished = true OR (e.endAt IS NOT NULL AND e.endAt >= CURRENT_DATE) OR (e.endAt IS NULL AND e.startAt >= CURRENT_DATE))
        AND (:searchKeyword IS NULL OR e.title LIKE %:searchKeyword% OR h.name LIKE %:searchKeyword%)
        AND (:isBookmarked = false OR b.id IS NOT NULL)
        """
    )
    fun findWithFilter(
        type: List <EventType>?,
        hostId: Long?,
        isApproved: Boolean,
        isBookmarked: Boolean,
        includeFinished: Boolean,
        searchKeyword: String?,
        userId: Long?,
        pageable: Pageable
    ): Page<Event>

    @Query(
        """
        SELECT e.id
        FROM Event e
        JOIN Bookmark b ON b.event.id = e.id
        WHERE b.user.id = :userId
        AND e.id IN :eventIds
        """
    )
    fun findBookmarkedEventIds(userId: Long, eventIds: List<Long>): List<Long>

    @Query(
        """
        SELECT DISTINCT e
        FROM Event e
        JOIN FETCH e.host
        JOIN FETCH e.view
        JOIN Bookmark b ON b.event = e
        WHERE b.user.id = :userId
        AND e.isApproved = true
        AND e.startAt BETWEEN :startDate AND :endDate
        AND (:eventType IS NULL OR e.eventType = :eventType)
        ORDER BY e.startAt ASC
        """
    )
    fun findEvents4Calendar(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        eventType: EventType?
    ): List<Event>
}