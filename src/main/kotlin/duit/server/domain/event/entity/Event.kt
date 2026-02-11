package duit.server.domain.event.entity

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.bookmark.entity.Bookmark
import duit.server.domain.event.dto.EventUpdateRequest
import duit.server.domain.host.entity.Host
import duit.server.domain.view.entity.View
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(
    name = "events", indexes = [
        Index(name = "idx_status_id", columnList = "status, id DESC"),
        Index(name = "idx_status_start_at_asc_id", columnList = "status, start_at ASC, id DESC"),
        Index(name = "idx_status_start_at_desc_id", columnList = "status, start_at DESC, id DESC"),
        Index(name = "idx_status_recruitment_asc_id", columnList = "status, recruitment_end_at ASC, id DESC"),
        Index(name = "idx_status_recruitment_desc_id", columnList = "status, recruitment_end_at DESC, id DESC"),

        Index(name = "idx_status_group_id", columnList = "status_group, id DESC"),
        Index(name = "idx_status_group_start_at_asc_id", columnList = "status_group, start_at ASC, id DESC"),
        Index(name = "idx_status_group_start_at_desc_id", columnList = "status_group, start_at DESC, id DESC"),
        Index(
            name = "idx_status_group_recruitment_asc_id",
            columnList = "status_group, recruitment_end_at ASC, id DESC"
        ),
        Index(
            name = "idx_status_group_recruitment_desc_id",
            columnList = "status_group, recruitment_end_at DESC, id DESC"
        ),
    ]
)
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var title: String,
    var startAt: LocalDateTime,
    var endAt: LocalDateTime?,
    var recruitmentStartAt: LocalDateTime?,
    var recruitmentEndAt: LocalDateTime?,
    @Column(columnDefinition = "TEXT")
    var uri: String,
    @Column(columnDefinition = "TEXT")
    var thumbnail: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.PENDING,

    @Enumerated(EnumType.STRING)
    var statusGroup: EventStatusGroup = EventStatusGroup.PENDING,

    @Enumerated(EnumType.STRING)
    var eventType: EventType,

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    var host: Host,

    @OneToOne(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    val view: View? = null,

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookmarks: List<Bookmark> = emptyList(),

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    val alarms: List<Alarm> = emptyList()
) {
    fun update(updateRequest: EventUpdateRequest, thumbnailUrl: String?, host: Host) {
        title = updateRequest.title
        startAt = updateRequest.startAt
        endAt = updateRequest.endAt
        recruitmentStartAt = updateRequest.recruitmentStartAt
        recruitmentEndAt = updateRequest.recruitmentEndAt
        uri = updateRequest.uri
        eventType = updateRequest.eventType
        this.host = host
        thumbnail = thumbnailUrl
    }

    fun updateStatus(time: LocalDateTime) {
        status = when {
            // 1. 이미 종료됨
            isFinished(time) -> EventStatus.FINISHED

            // 2. 행사 진행 중
            isActive(time) -> EventStatus.ACTIVE

            // 3. 모집 없이 행사 대기, 4. 모집 기간 지남
            isEventWaiting(time) -> EventStatus.EVENT_WAITING

            // 5. 모집 중
            isRecruiting(time) -> EventStatus.RECRUITING

            // 6. 모집 대기
            isRecruitmentWaiting(time) -> EventStatus.RECRUITMENT_WAITING

            else -> error("잘못된 행사 상태: startAt=$startAt, endAt=$endAt, recruitmentStartAt=$recruitmentStartAt, recruitmentEndAt=$recruitmentEndAt, time=$time")
        }

        statusGroup = when (status) {
            EventStatus.PENDING -> EventStatusGroup.PENDING
            EventStatus.FINISHED -> EventStatusGroup.FINISHED
            else -> EventStatusGroup.ACTIVE
        }
    }

    private fun isFinished(time: LocalDateTime): Boolean =
        time > (endAt ?: startAt)

    private fun isActive(time: LocalDateTime): Boolean =
        time in startAt..(endAt ?: startAt)

    private fun isEventWaiting(time: LocalDateTime): Boolean =
        (recruitmentStartAt == null && recruitmentEndAt == null) ||
                recruitmentEndAt?.let { it <= time } ?: false

    private fun isRecruiting(time: LocalDateTime): Boolean {
        val start = recruitmentStartAt
        val end = recruitmentEndAt

        return when {
            // 시작/종료일 모두 있는 경우
            start != null && end != null -> time in start..end
            // 종료일만 있는 경우 (오늘부터 종료일까지 모집)
            start == null && end != null -> time <= end
            // 시작일만 있는 경우 (시작일부터 행사 전까지 모집)
            start != null && end == null -> time in start..<startAt
            else -> false
        }
    }

    private fun isRecruitmentWaiting(time: LocalDateTime): Boolean =
        recruitmentStartAt?.let { it >= time } ?: false
}