package duit.server.domain.bookmark.entity

import duit.server.domain.event.entity.Event
import duit.server.domain.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "bookmarks",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "event_id"])]

)
@EntityListeners(AuditingEntityListener::class)
class Bookmark(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    val event: Event,

    var isAddedToCalendar: Boolean = false,

    @CreatedDate
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
}