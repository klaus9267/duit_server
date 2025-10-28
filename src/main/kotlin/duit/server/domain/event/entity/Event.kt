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
@Table(name = "events")
@EntityListeners(AuditingEntityListener::class)
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
}