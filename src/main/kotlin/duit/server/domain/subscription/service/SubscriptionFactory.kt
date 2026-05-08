package duit.server.domain.subscription.service

import duit.server.domain.subscription.dto.SubscriptionCreateRequest
import duit.server.domain.subscription.dto.SubscriptionResponse
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.user.entity.User

/**
 * [SubscriptionType] 별 생성/응답 변환 전략.
 * 새 type 추가 시 본 인터페이스 구현체 1개만 추가하면 [SubscriptionService] 가 자동 분기.
 */
interface SubscriptionFactory {
    val type: SubscriptionType
    fun create(user: User, request: SubscriptionCreateRequest): Subscription
    fun toResponse(subscription: Subscription): SubscriptionResponse
}
