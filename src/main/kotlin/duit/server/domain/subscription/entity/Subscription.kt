package duit.server.domain.subscription.entity

import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import duit.server.domain.job.entity.Company
import duit.server.domain.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 구독 엔티티 — 5종 [SubscriptionType] 의 폴리모픽 row.
 *
 * type 별로 채워지는 컬럼이 다름:
 *  - [SubscriptionType.EVENT_KEYWORD] / [SubscriptionType.JOB_KEYWORD] → [keyword] 만
 *  - [SubscriptionType.EVENT_HOST]   → [host] 만
 *  - [SubscriptionType.EVENT_TYPE]   → [eventType] 만
 *  - [SubscriptionType.JOB_COMPANY]  → [company] 만
 *
 * invariant 는 [init] 블록에서 강제. 잘못된 조합으로 생성하면 [IllegalArgumentException].
 *
 * 동일 사용자의 같은 (type + 대상) 중복 구독 방지는 SubscriptionService 의 existsBy... 명시 체크로 처리
 * (MySQL UNIQUE 가 nullable 컬럼에서 NULL distinct 라 DB 단 보장 어려움).
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "subscriptions")
class Subscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: SubscriptionType,

    @Column(length = 50)
    val keyword: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    val host: Host? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 30)
    val eventType: EventType? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    val company: Company? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    init {
        when (type) {
            SubscriptionType.EVENT_KEYWORD,
            SubscriptionType.JOB_KEYWORD -> {
                require(!keyword.isNullOrBlank()) { "$type 구독에는 keyword 가 필요합니다" }
                require(host == null && eventType == null && company == null) {
                    "$type 구독에는 keyword 외 다른 대상 필드가 채워지면 안 됩니다"
                }
            }

            SubscriptionType.EVENT_HOST -> {
                require(host != null) { "EVENT_HOST 구독에는 host 가 필요합니다" }
                require(keyword == null && eventType == null && company == null) {
                    "EVENT_HOST 구독에는 host 외 다른 대상 필드가 채워지면 안 됩니다"
                }
            }

            SubscriptionType.EVENT_TYPE -> {
                require(eventType != null) { "EVENT_TYPE 구독에는 eventType 이 필요합니다" }
                require(keyword == null && host == null && company == null) {
                    "EVENT_TYPE 구독에는 eventType 외 다른 대상 필드가 채워지면 안 됩니다"
                }
            }

            SubscriptionType.JOB_COMPANY -> {
                require(company != null) { "JOB_COMPANY 구독에는 company 가 필요합니다" }
                require(keyword == null && host == null && eventType == null) {
                    "JOB_COMPANY 구독에는 company 외 다른 대상 필드가 채워지면 안 됩니다"
                }
            }
        }
    }
}
