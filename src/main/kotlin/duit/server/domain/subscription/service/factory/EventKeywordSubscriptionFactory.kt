package duit.server.domain.subscription.service.factory

import duit.server.domain.subscription.dto.EventKeywordSubscriptionResponse
import duit.server.domain.subscription.dto.SubscriptionCreateRequest
import duit.server.domain.subscription.dto.SubscriptionResponse
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.repository.SubscriptionRepository
import duit.server.domain.subscription.service.SubscriptionFactory
import duit.server.domain.user.entity.User
import org.springframework.stereotype.Component

@Component
class EventKeywordSubscriptionFactory(
    private val subscriptionRepository: SubscriptionRepository,
) : SubscriptionFactory {

    override val type = SubscriptionType.EVENT_KEYWORD

    override fun create(user: User, request: SubscriptionCreateRequest): Subscription {
        val keyword = request.keyword?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("EVENT_KEYWORD 구독에는 keyword 가 필요합니다")
        require(!subscriptionRepository.existsByUserIdAndTypeAndKeyword(user.id!!, type, keyword)) {
            "이미 같은 키워드로 구독 중입니다"
        }
        return Subscription(user = user, type = type, keyword = keyword)
    }

    override fun toResponse(subscription: Subscription): SubscriptionResponse =
        EventKeywordSubscriptionResponse(
            id = subscription.id!!,
            keyword = subscription.keyword!!,
            createdAt = subscription.createdAt,
        )
}
