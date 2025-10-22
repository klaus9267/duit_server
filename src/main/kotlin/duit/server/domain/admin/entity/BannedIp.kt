package duit.server.domain.admin.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "banned_ips")
@EntityListeners(AuditingEntityListener::class)
data class BannedIp(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 45)
    val ipAddress: String,

    @Column(nullable = false)
    var failureCount: Int = 0,

    @Column(nullable = false)
    var isBanned: Boolean = false,

    @Column(nullable = false)
    var lastFailureAt: LocalDateTime = LocalDateTime.now(),

    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun ban() {
        isBanned = true
    }

    fun unban() {
        isBanned = false
        failureCount = 0
    }

    fun recordFailure() {
        val now = LocalDateTime.now()

        if (lastFailureAt.plusHours(24).isAfter(now)) {
            failureCount++
        } else {
            failureCount = 1
        }

        lastFailureAt = now

        // 5회 이상 실패 시 ban
        if (failureCount >= 5 && !isBanned) {
            ban()
        }
    }
}
