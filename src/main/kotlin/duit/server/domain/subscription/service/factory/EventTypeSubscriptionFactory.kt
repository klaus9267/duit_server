package duit.server.domain.subscription.service.factory

import duit.server.domain.subscription.dto.EventTypeSubscriptionResponse
import duit.server.domain.subscription.dto.SubscriptionCreateRequest
import duit.server.domain.subscription.dto.SubscriptionResponse
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.repository.SubscriptionRepository
import duit.server.domain.subscription.service.SubscriptionFactory
import duit.server.domain.user.entity.User
import org.springframework.stereotype.Component

@Component
class EventTypeSubscriptionFactory(
    private val subscriptionRepository: SubscriptionRepository,
) : SubscriptionFactory {

    override val type = SubscriptionType.EVENT_TYPE

    override fun create(user: User, request: SubscriptionCreateRequest): Subscription {
        val eventType = request.eventType
            ?: throw IllegalArgumentException("EVENT_TYPE 구독에는 eventType 이 필요합니다")
        require(!subscriptionRepository.existsByUserIdAndTypeAndEventType(user.id!!, type, eventType)) {
            "이미 해당 행사 유형을 구독 중입니다"
        }
        return Subscription(user = user, type = type, eventType = eventType)
    }

    override fun toResponse(subscription: Subscription): SubscriptionResponse =
        EventTypeSubscriptionResponse(
            id = subscription.id!!,
            eventType = subscription.eventType!!,
            createdAt = subscription.createdAt,
        )
}
