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
    // 목록 쿼리의 event_hosts 존재 조건은 주최 없는 행사를 hosts inner join 시절처럼 제외한다 (EventRepositoryImpl 도 같음)
    @Query(
        value = """
        SELECT e.*
        FROM events e
        LEFT JOIN views v ON v.event_id = e.id
        LEFT JOIN bookmarks b ON b.event_id = e.id AND (b.user_id = :#{#filter.userId} OR :#{#filter.userId} IS NULL)
        WHERE EXISTS (SELECT 1 FROM event_hosts eh0 WHERE eh0.event_id = e.id)
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
        WHERE EXISTS (SELECT 1 FROM event_hosts eh0 WHERE eh0.event_id = e.id)
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
        SELECT DISTINCT e
        FROM Event e
        LEFT JOIN FETCH e.eventHosts eh
        LEFT JOIN FETCH eh.host
        JOIN FETCH e.view
        JOIN Bookmark b ON b.event = e
        WHERE b.user.id = :userId
        AND EXISTS (SELECT 1 FROM EventHost eh0 WHERE eh0.event = e)
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

    /**
     * 행사 상세: 행사·주최·조회수를 SQL 1개로 (performance D-026).
     * 주최는 지연 로딩이라 1번, 조회수(@OneToOne mappedBy)는 프록시를 못 만들어 즉시 로딩으로 또 1번 따로 읽었다.
     * 다중 주최 이후 응답의 주최는 event_hosts 에서 온다 — 주최 연결과 그 주최까지 같이 읽는다(단건이라 컬렉션 fetch join 의 페이징 문제가 없다).
     */
    @EntityGraph(attributePaths = ["eventHosts", "eventHosts.host", "view"])
    fun findWithHostById(id: Long): Event?

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