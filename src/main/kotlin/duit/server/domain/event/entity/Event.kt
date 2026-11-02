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

    // var 필수: val 이면 Spring Data auditing 이 primary constructor 로 재인스턴스화를 시도하는데,
    // 생성자 파라미터 hosts 는 영속 프로퍼티가 아니어서 "Required property hosts not found" 로 실패한다
    @CreatedDate
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    hosts: List<Host>,

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookmarks: List<Bookmark> = emptyList(),

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    val alarms: List<Alarm> = emptyList()
) {
    @OneToOne(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    var view: View = View(event = this)

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private val eventHosts: MutableList<EventHost> = mutableListOf()

    // TODO(event-hosts): events.host_id 는 blue-green 배포 중 구 서버·롤백 호환을 위해 첫 번째 주최를 미러링만 한다.
    //  읽기는 항상 hosts 를 사용할 것. 구 서버가 모두 내려간 뒤 events.host_id 컬럼(V6 마이그레이션)과 함께 이 필드를 제거.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    var host: Host = requireNotNull(hosts.firstOrNull()) { "주최는 최소 1개 이상이어야 합니다" }
        protected set

    // TODO(event-hosts): ifEmpty fallback 은 전환 구간에 구 서버가 event_hosts 없이 기록한 행 대응. V6 백필 후 제거
    val hosts: List<Host>
        get() = eventHosts.sortedBy { it.sortOrder }.map { it.host }.ifEmpty { listOf(host) }

    init {
        replaceHosts(hosts)
    }

    fun replaceHosts(newHosts: List<Host>) {
        val orderedHosts = newHosts.distinctBy { it.identity() }
        require(orderedHosts.isNotEmpty()) { "주최는 최소 1개 이상이어야 합니다" }

        val keptIdentities = orderedHosts.map { it.identity() }.toSet()
        eventHosts.removeIf { it.host.identity() !in keptIdentities }

        orderedHosts.forEachIndexed { index, host ->
            eventHosts.find { it.host.identity() == host.identity() }
                ?.let { it.sortOrder = index }
                ?: eventHosts.add(EventHost(event = this, host = host, sortOrder = index))
        }

        host = orderedHosts.first()
    }

    private fun Host.identity(): Any = id ?: this

    fun update(updateRequest: EventUpdateRequest, thumbnailUrl: String?, hosts: List<Host>) {
        title = updateRequest.title
        startAt = updateRequest.startAt
        endAt = updateRequest.endAt
        recruitmentStartAt = updateRequest.recruitmentStartAt
        recruitmentEndAt = updateRequest.recruitmentEndAt
        uri = updateRequest.uri
        eventType = updateRequest.eventType
        replaceHosts(hosts)
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