package duit.server.domain.alarm.repository

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AlarmRepository : JpaRepository<Alarm, Long> {

    fun existsByUserIdAndEventIdAndType(userId: Long, eventId: Long, type: AlarmType): Boolean

    fun existsByUserIdAndJobPostingIdAndType(userId: Long, jobPostingId: Long, type: AlarmType): Boolean

    // ─────────────────────────────────────────────────────────────────────
    // V1 호환용 — 이벤트 알람만 (event IS NOT NULL)
    // ─────────────────────────────────────────────────────────────────────
    @EntityGraph(attributePaths = ["event", "event.host"])
    fun findByUserIdAndEventIsNotNull(userId: Long, pageable: Pageable): Page<Alarm>

    @EntityGraph(attributePaths = ["event", "event.host"])
    fun findByUserIdAndEventIsNotNullAndIsRead(userId: Long, isRead: Boolean, pageable: Pageable): Page<Alarm>

    // ─────────────────────────────────────────────────────────────────────
    // V2 — 모든 알람 (event 또는 jobPosting). Phase 3 에서 사용 시작
    // ─────────────────────────────────────────────────────────────────────
    @EntityGraph(attributePaths = ["event", "event.host", "jobPosting", "jobPosting.company"])
    fun findByUserId(userId: Long, pageable: Pageable): Page<Alarm>

    @EntityGraph(attributePaths = ["event", "event.host", "jobPosting", "jobPosting.company"])
    fun findByUserIdAndId(userId: Long, id: Long): Alarm?

    // 목록 응답에 공고 내용이 담기므로, 알람 생성 뒤 최저임금 미달로 바뀐 공고의 알람은 제외한다
    @EntityGraph(attributePaths = ["event", "event.host", "jobPosting", "jobPosting.company"])
    @Query(
        """
        SELECT a FROM Alarm a LEFT JOIN a.jobPosting jp
        WHERE a.user.id = :userId AND (jp IS NULL OR jp.belowMinimumWage = false)
        """
    )
    fun findPublishableByUserId(@Param("userId") userId: Long, pageable: Pageable): Page<Alarm>

    @EntityGraph(attributePaths = ["event", "event.host", "jobPosting", "jobPosting.company"])
    @Query(
        """
        SELECT a FROM Alarm a LEFT JOIN a.jobPosting jp
        WHERE a.user.id = :userId AND a.isRead = :isRead AND (jp IS NULL OR jp.belowMinimumWage = false)
        """
    )
    fun findPublishableByUserIdAndIsRead(
        @Param("userId") userId: Long,
        @Param("isRead") isRead: Boolean,
        pageable: Pageable,
    ): Page<Alarm>

    @Modifying
    fun deleteByUserId(userId: Long)

    @Modifying
    fun deleteByUserIdAndIsRead(userId: Long, isRead: Boolean)
}
