package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventSearchFilter
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface EventRepository : JpaRepository<Event, Long>, EventRepositoryCustom {

    // todo - v2로 옮기면 삭제 예정
    // TODO(P-13): 목록 쿼리의 host_id IS NOT NULL 은 hosts inner join 시절처럼 주최 없는 행사를 제외하기 위한 조건.
    //  host_id 사용을 중단할 때 event_hosts 존재 조건으로 바꾼다 (이 파일과 EventRepositoryImpl 모두)
    @Query(
        value = """
        SELECT e.*
        FROM events e
        LEFT JOIN views v ON v.event_id = e.id
        LEFT JOIN bookmarks b ON b.event_id = e.id AND (b.user_id = :#{#filter.userId} OR :#{#filter.userId} IS NULL)
        WHERE e.host_id IS NOT NULL
        AND (:#{#filter.excludePending} = false OR e.status_group <> 'PENDING')
        AND (:#{#filter.eventTypesToString()} IS NULL OR FIND_IN_SET(e.event_type, :#{#filter.eventTypesToString()}) > 0)
        AND (:#{#filter.includeFinished} = 1 OR (e.end_at IS NOT NULL AND e.end_at <= CURDATE()) OR (e.end_at IS NULL AND e.start_at <= CURDATE()))
        AND (:#{#filter.isBookmarked} = 0 OR b.id IS NOT NULL)
        ORDER BY
            CASE
                WHEN e.start_at >= CURDATE() THEN 0
                ELSE 1
            END ASC,
            CASE
                WHEN :#{#filter.sortFieldName()} = 'startAt' THEN
                    ABS(TIMESTAMPDIFF(SECOND, NOW(), e.start_at))
                WHEN :#{#filter.sortFieldName()} = 'recruitmentEndAt' THEN 
                    (CASE 
                        WHEN e.recruitment_end_at IS NULL 
                            THEN 999999999 
                            ELSE TIMESTAMPDIFF(SECOND, NOW(), e.recruitment_end_at) 
                    END)
                WHEN :#{#filter.sortFieldName()} = 'view.count' THEN
                    -COALESCE(v.count, 0)
                WHEN :#{#filter.sortFieldName()} = 'createdAt' THEN
                    -UNIX_TIMESTAMP(e.created_at)
                ELSE -e.id
            END ASC
        """,
        nativeQuery = true,
        countQuery = """
        SELECT COUNT(DISTINCT e.id)
        FROM events e
        LEFT JOIN bookmarks b ON b.event_id = e.id AND (b.user_id = :#{#filter.userId} OR :#{#filter.userId} IS NULL)
        WHERE e.host_id IS NOT NULL
        AND (:#{#filter.excludePending} = false OR e.status_group <> 'PENDING')
        AND (:#{#filter.eventTypesToString()} IS NULL OR FIND_IN_SET(e.event_type, :#{#filter.eventTypesToString()}) > 0)
        AND (:#{#filter.includeFinished} = 1 OR (e.end_at IS NOT NULL AND e.end_at <= CURDATE()) OR (e.end_at IS NULL AND e.start_at <= CURDATE()))
        AND (:#{#filter.isBookmarked} = 0 OR b.id IS NOT NULL)
        """
    )
    fun findWithFilter(
        filter: EventSearchFilter,
        pageable: Pageable
    ): Page<Event>

    @Query(
        """
      SELECT e
      FROM Event e
      WHERE e.status <> duit.server.domain.event.entity.EventStatus.PENDING
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
        LEFT JOIN FETCH e.eventHosts eh
        LEFT JOIN FETCH eh.host
        JOIN FETCH e.view
        JOIN Bookmark b ON b.event = e
        WHERE b.user.id = :userId
        AND e.host IS NOT NULL
        AND e.status != EventStatus.PENDING
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

    @Query(
        """
            SELECT COUNT(DISTINCT e.id)
            FROM Event e
            WHERE e.statusGroup = ACTIVE
            OR e.statusGroup = FINISHED
        """
    )
    fun countActiveEvents(): Long

    @Query("SELECT CASE WHEN COUNT(eh) > 0 THEN true ELSE false END FROM EventHost eh WHERE eh.host.id = :hostId")
    fun existsByHostId(@Param("hostId") hostId: Long): Boolean

    @Query("SELECT DISTINCT eh.host.id FROM EventHost eh WHERE eh.host.id IN :hostIds")
    fun findHostIdsWithEvents(@Param("hostIds") hostIds: List<Long>): List<Long>
}