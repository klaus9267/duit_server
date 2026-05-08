package duit.server.domain.subscription.repository

import duit.server.domain.event.entity.EventType
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionRepository : JpaRepository<Subscription, Long> {

    // 사용자 본인 구독 목록
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<Subscription>
    fun findAllByUserIdAndTypeOrderByIdDesc(userId: Long, type: SubscriptionType): List<Subscription>

    // 본인 소유 구독인지 확인 (DELETE 권한 체크용)
    fun findByIdAndUserId(id: Long, userId: Long): Subscription?

    // 중복 구독 방지 (SubscriptionService.create 시 사용)
    fun existsByUserIdAndTypeAndKeyword(userId: Long, type: SubscriptionType, keyword: String): Boolean
    fun existsByUserIdAndTypeAndHostId(userId: Long, type: SubscriptionType, hostId: Long): Boolean
    fun existsByUserIdAndTypeAndEventType(userId: Long, type: SubscriptionType, eventType: EventType): Boolean
    fun existsByUserIdAndTypeAndCompanyId(userId: Long, type: SubscriptionType, companyId: Long): Boolean

    // 매칭 (행사/채용 등록 시 구독자 찾기 — Phase 4 Matcher 에서 사용)
    fun findAllByTypeAndHostId(type: SubscriptionType, hostId: Long): List<Subscription>
    fun findAllByTypeAndEventType(type: SubscriptionType, eventType: EventType): List<Subscription>
    fun findAllByTypeAndCompanyId(type: SubscriptionType, companyId: Long): List<Subscription>
    fun findAllByType(type: SubscriptionType): List<Subscription>  // KEYWORD 매칭용 (앱에서 contains 검사)
}
