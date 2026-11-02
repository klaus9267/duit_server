package duit.server.domain.event.entity

import duit.server.domain.host.entity.Host
import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
    name = "event_hosts",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_hosts_event_host", columnNames = ["event_id", "host_id"])
    ],
    indexes = [
        Index(name = "idx_event_hosts_host_event", columnList = "host_id, event_id")
    ]
)
class EventHost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val event: Event,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    val host: Host,

    @Column(nullable = false)
    var sortOrder: Int,
)
