package duit.server.domain.event.repository

import duit.server.domain.event.dto.EventSearchFilter
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.time.LocalDateTime

interface EventRepository : JpaRepository<Event, Long> {
    @Query(
        value = """
        SELECT e.* 
        FROM events e
        JOIN hosts h ON h.id = e.host_id
        LEFT JOIN views v ON v.event_id = e.id
        LEFT JOIN bookmarks b ON b.event_id = e.id AND (b.user_id = :#{#filter.userId} OR :#{#filter.userId} IS NULL)
        WHERE e.is_approved = :#{#filter.isApproved}
        AND (:#{#filter.hostId} IS NULL OR h.id = :#{#filter.hostId})
        AND (:#{#filter.eventTypes} IS NULL OR FIND_IN_SET(e.event_type, :#{#filter.eventTypes}) > 0)
        AND (:#{#filter.includeFinished} = 1 OR (e.end_at IS NOT NULL AND e.end_at >= CURDATE()) OR (e.end_at IS NULL AND e.start_at >= CURDATE()))
        AND (:#{#filter.searchKeyword} IS NULL OR e.title LIKE CONCAT('%', :#{#filter.searchKeyword}, '%') OR h.name LIKE CONCAT('%', :#{#filter.searchKeyword}, '%'))
        AND (:#{#filter.isBookmarked} = 0 OR b.id IS NOT NULL)
        GROUP BY e.id
        ORDER BY 
            -- 1순위: 진행 상태 (진행중/예정이 먼저)
            CASE 
                WHEN (e.end_at IS NOT NULL AND e.end_at >= CURDATE()) OR (e.start_at IS NULL AND e.start_at >= CURDATE()) THEN 0
                ELSE 1
            END ASC,
            -- 2순위: 사용자 지정 정렬 (음수 처리로 동적 정렬)
            CASE 
                WHEN :#{#filter.sortField} = 'startat' THEN 
                    CASE WHEN :#{#filter.sortDirection} = 'desc' THEN -ABS(DATEDIFF(e.start_at, CURDATE())) 
                         ELSE ABS(DATEDIFF(e.start_at, CURDATE())) 
                    END
                WHEN :#{#filter.sortField} = 'recruitmentendat' THEN 
                    CASE WHEN :#{#filter.sortDirection} = 'desc' THEN 
                        -(CASE WHEN e.recruitment_end_at IS NULL THEN 999999999 ELSE TIMESTAMPDIFF(SECOND, NOW(), e.recruitment_end_at) END)
                         ELSE 
                        (CASE WHEN e.recruitment_end_at IS NULL THEN 999999999 ELSE TIMESTAMPDIFF(SECOND, NOW(), e.recruitment_end_at) END)
                    END
                WHEN :#{#filter.sortField} = 'view.count' THEN 
                    CASE WHEN :#{#filter.sortDirection} = 'desc' THEN -COALESCE(v.count, 0)
                         ELSE COALESCE(v.count, 0)
                    END
                ELSE 
                    CASE WHEN :#{#filter.sortDirection} = 'desc' THEN -e.id
                         ELSE e.id
                    END
            END ASC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT e.id)
        FROM events e
        JOIN hosts h ON h.id = e.host_id
        LEFT JOIN bookmarks b ON b.event_id = e.id AND (b.user_id = :#{#filter.userId} OR :#{#filter.userId} IS NULL)
        WHERE e.is_approved = :#{#filter.isApproved} 
        AND (:#{#filter.hostId} IS NULL OR h.id = :#{#filter.hostId})
        AND (:#{#filter.eventTypes} IS NULL OR FIND_IN_SET(e.event_type, :#{#filter.eventTypes}) > 0)
        AND (:#{#filter.includeFinished} = 1 OR (e.end_at IS NOT NULL AND e.end_at >= CURDATE()) OR (e.end_at IS NULL AND e.start_at >= CURDATE()))
        AND (:#{#filter.searchKeyword} IS NULL OR e.title LIKE CONCAT('%', :#{#filter.searchKeyword}, '%') OR h.name LIKE CONCAT('%', :#{#filter.searchKeyword}, '%'))
        AND (:#{#filter.isBookmarked} = 0 OR b.id IS NOT NULL)
        """,
        nativeQuery = true
    )
    fun findWithFilter(
        filter: EventSearchFilter,
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

    @Query(
        """
        SELECT DISTINCT e
        FROM Event e
        JOIN FETCH e.host
        WHERE e.isApproved = true
        AND e.startAt = :tomorrow
        ORDER BY e.startAt ASC
        """
    )
    fun findEventsStartingTomorrow(tomorrow: LocalDate): List<Event>

    @Query(
        """
        SELECT DISTINCT e
        FROM Event e
        JOIN FETCH e.host
        WHERE e.isApproved = true
        AND e.recruitmentStartAt >= :tomorrow
        AND e.recruitmentStartAt < :nextDay
        AND e.recruitmentStartAt IS NOT NULL
        ORDER BY e.recruitmentStartAt ASC
        """
    )
    fun findRecruitmentStartingTomorrow(tomorrow: LocalDateTime, nextDay: LocalDateTime): List<Event>

    @Query(
        """
        SELECT DISTINCT e
        FROM Event e
        JOIN FETCH e.host
        WHERE e.isApproved = true
        AND e.recruitmentEndAt >= :tomorrow
        AND e.recruitmentEndAt < :nextDay
        AND e.recruitmentEndAt IS NOT NULL
        ORDER BY e.recruitmentEndAt ASC
        """
    )
    fun findRecruitmentEndingTomorrow(tomorrow: LocalDateTime, nextDay: LocalDateTime): List<Event>
}