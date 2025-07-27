package duit.server.domain.event.entity

import duit.server.domain.host.entity.Host
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "events")
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val title: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val recruitmentStartAt: LocalDateTime,
    val recruitmentEndAt: LocalDateTime,
    val uri: String,

    @Enumerated(EnumType.STRING)
    val eventType: EventType,

    @ManyToOne(fetch = FetchType.LAZY)
    val host: Host
) {
}