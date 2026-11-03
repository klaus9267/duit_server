package duit.server.domain.bookmark.repository

import duit.server.domain.bookmark.entity.Bookmark
import duit.server.domain.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface BookmarkRepository : JpaRepository<Bookmark, Long> {
    fun findByEventIdAndUserId(eventId: Long, userId: Long): Bookmark?

    /**
     * 북마크 ID 캐시 적재용. (user_id, event_id) 유니크 인덱스만 읽는다.
     * 트랜잭션 밖(행사 조회 조립 중 캐시 미스)에서도 불리므로 자체 읽기 트랜잭션으로 커넥션을 조회 직후 반납한다 (performance D-027)
     */
    @Transactional(readOnly = true)
    @Query("SELECT b.event.id FROM Bookmark b WHERE b.user.id = :userId")
    fun findEventIdsByUserId(userId: Long): List<Long>

    @EntityGraph(attributePaths = ["event"])
    fun findByUserId(userId: Long, pageable: Pageable): Page<Bookmark>

    @Query(
        """
        SELECT DISTINCT u
        FROM User u
        JOIN Bookmark b ON b.user.id = u.id
        JOIN FETCH u.deviceTokens udt
        WHERE b.event.id = :eventId
          AND u.alarmSettings.push = true
          AND (
            u.alarmSettings.bookmark = true
            OR (u.alarmSettings.calendar = true AND b.isAddedToCalendar = true)
          )
        """
    )
    fun findEligibleUsersForAlarms(eventId: Long): List<User>
}
