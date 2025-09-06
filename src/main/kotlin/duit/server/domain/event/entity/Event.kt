package duit.server.domain.event.entity

import duit.server.domain.host.entity.Host
import duit.server.domain.view.entity.View
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "events")
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val title: String,
    val startAt: LocalDate,
    val endAt: LocalDate?,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val uri: String,
    val thumbnail: String?,
    var isApproved: Boolean = false,

    @Enumerated(EnumType.STRING)
    val eventType: EventType,

    @ManyToOne(fetch = FetchType.LAZY)
    val host: Host,

    @OneToOne(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    val view: View? = null
) {
}