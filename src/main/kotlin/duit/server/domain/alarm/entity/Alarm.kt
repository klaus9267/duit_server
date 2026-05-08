package duit.server.domain.alarm.entity

import duit.server.domain.event.entity.Event
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 알람 — 이벤트 알람 또는 채용공고 알람.
 *
 * - 북마크 기반 이벤트 알람: [event] 채워짐, [jobPosting] = null
 * - 구독 기반 이벤트 알람:   [event] 채워짐, [jobPosting] = null
 * - 구독 기반 채용 알람:    [event] = null,   [jobPosting] 채워짐
 *
 * `event` / `jobPosting` 둘 중 정확히 하나만 채워져야 함 — [init] 에서 강제.
 *
 * UNIQUE (user_id, event_id, type) 는 이벤트 알람 dedup 용으로 유지.
 * 채용 알람(event_id IS NULL) 은 NULL distinct 로 DB dedup 미작동 → AlarmService 의 existsBy... 로 처리.
 */
@Entity
@Table(
    name = "alarms",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_event_type", columnNames = ["user_id", "event_id", "type"])
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Alarm(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    val event: Event? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id")
    val jobPosting: JobPosting? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: AlarmType,

    @Column(nullable = false)
    var isRead: Boolean = false,

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    init {
        require((event != null) xor (jobPosting != null)) {
            "Alarm 은 event 또는 jobPosting 중 정확히 하나만 가져야 합니다 (현재: event=$event, jobPosting=$jobPosting)"
        }
    }
}