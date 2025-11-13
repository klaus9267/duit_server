package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventSearchFilter
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface EventRepository : JpaRepository<Event, Long>, EventRepositoryCustom {

    @Query(
        """
        SELECT e
        FROM Event e
        JOIN FETCH e.host h
        JOIN FETCH e.view v
        WHERE e.isApproved = TRUE
        AND e.title LIKE CONCAT('%', :keyword, '%')
        OR h.name LIKE CONCAT('%', :keyword, '%')
        """
    )
    fun searchEvents(keyword: String): List<Event>

    @Query(
        """
      SELECT e
      FROM Event e
      WHERE e.isApproved = true
      AND (
          (:fieldName = 'START_AT' AND e.startAt >= :tomorrow AND e.startAt < :nextDay) OR
          (:fieldName = 'RECRUITMENT_START_AT' AND e.recruitmentStartAt >= :tomorrow AND e.recruitmentStartAt < :nextDay) OR
          (:fieldName = 'RECRUITMENT_END_AT' AND e.recruitmentEndAt >= :tomorrow AND e.recruitmentEndAt < :nextDay)
      )
  """
    )
    fun findEventsByDateField(
        fieldName: String,
        tomorrow: LocalDateTime,
        nextDay: LocalDateTime
    ): List<Event>


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
        AND e.startAt BETWEEN :start AND :end
        AND (:eventType IS NULL OR e.eventType = :eventType)
        ORDER BY e.startAt ASC
        """
    )
    fun findEvents4Calendar(
        userId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
        eventType: EventType?
    ): List<Event>

    @EntityGraph(attributePaths = ["view"])
    fun findAllByIdInAndThumbnailNotNull(ids: List<Long>): List<Event>
}