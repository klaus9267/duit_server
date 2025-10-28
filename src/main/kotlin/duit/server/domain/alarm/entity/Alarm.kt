package duit.server.domain.alarm.entity

import duit.server.domain.event.entity.Event
import duit.server.domain.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "alarms")
@EntityListeners(AuditingEntityListener::class)
class Alarm(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    val event: Event,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: AlarmType,

    @Column(nullable = false)
    var isRead: Boolean = false,

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now()
)