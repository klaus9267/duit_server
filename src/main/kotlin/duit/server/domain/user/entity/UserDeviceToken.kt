package duit.server.domain.user.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "user_device_tokens")
@EntityListeners(AuditingEntityListener::class)
class UserDeviceToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false, length = 512, unique = true)
    val token: String,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // 앱이 이 토큰을 마지막으로 등록한 시각. 사용자당 개수를 넘으면 이 값이 오래된 토큰부터 지운다
    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    // 앱은 실행할 때마다 같은 토큰을 다시 등록한다. 쓰기를 줄이려고 하루에 한 번만 갱신한다
    fun markRegistered(now: LocalDateTime = LocalDateTime.now()) {
        if (updatedAt.isBefore(now.minusDays(1))) updatedAt = now
    }

    fun transferTo(newOwner: User, now: LocalDateTime = LocalDateTime.now()) {
        user = newOwner
        updatedAt = now
    }
}
