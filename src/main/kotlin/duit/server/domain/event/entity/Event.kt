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
        Index(name = "idx_status_created_at_id", columnList = "status, created_at DESC, id DESC"),
        Index(name = "idx_status_start_at_asc_id", columnList = "status, start_at ASC, id DESC"),
        Index(name = "idx_status_start_at_desc_id", columnList = "status, start_at DESC, id DESC"),
        Index(name = "idx_status_recruitment_asc_id", columnList = "status, recruitment_end_at ASC, id DESC"),
        Index(name = "idx_status_recruitment_desc_id", columnList = "status, recruitment_end_at DESC, id DESC"),

        Index(name = "idx_status_group_created_at_id", columnList = "status_group, created_at DESC, id DESC"),
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
    var uri: String,
    var thumbnail: String?,
    var isApproved: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.PENDING,

    @Enumerated(EnumType.STRING)
    var statusGroup: EventStatusGroup = EventStatusGroup.PENDING,

    @Enumerated(EnumType.STRING)
    val eventType: EventType,

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
        this.host = host
        thumbnailUrl?.let { thumbnail = it }
    }

    fun updateStatus() {
        val now = LocalDateTime.now()

        when {
            // 1. 이미 종료됨
            (endAt != null && endAt!! < now) ||
                    (endAt == null && startAt < now) -> {
                status = EventStatus.FINISHED
                statusGroup = EventStatusGroup.FINISHED
            }

            // 2. 행사 진행 중
            startAt <= now && (endAt == null || endAt!! >= now) -> {
                status = EventStatus.ACTIVE
                statusGroup = EventStatusGroup.ACTIVE
            }

            // 3. 모집 없이 행사 대기, 4. 모집 기간 지남
            recruitmentStartAt == null ||
                    recruitmentEndAt!! < now -> {
                status = EventStatus.EVENT_WAITING
                statusGroup = EventStatusGroup.ACTIVE
            }

            // 5. 모집 중
            recruitmentStartAt!! <= now -> {
                status = EventStatus.RECRUITING
                statusGroup = EventStatusGroup.ACTIVE
            }

            // 6. 모집 대기
            else -> {
                status = EventStatus.RECRUITMENT_WAITING
                statusGroup = EventStatusGroup.ACTIVE
            }
        }
    }

    fun updateStatus(newStatus: EventStatus) {
        status = newStatus
        if (newStatus == EventStatus.FINISHED) {
            statusGroup = EventStatusGroup.FINISHED
        }
    }
}