package duit.server.domain.subscription.service.factory

import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.host.repository.HostRepository
import duit.server.domain.subscription.dto.EventHostSubscriptionResponse
import duit.server.domain.subscription.dto.SubscriptionCreateRequest
import duit.server.domain.subscription.dto.SubscriptionResponse
import duit.server.domain.subscription.dto.SubscriptionTargetHost
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.repository.SubscriptionRepository
import duit.server.domain.subscription.service.SubscriptionFactory
import duit.server.domain.user.entity.User
import org.springframework.stereotype.Component

@Component
class EventHostSubscriptionFactory(
    private val hostRepository: HostRepository,
    private val subscriptionRepository: SubscriptionRepository,
) : SubscriptionFactory {

    override val type = SubscriptionType.EVENT_HOST

    override fun create(user: User, request: SubscriptionCreateRequest): Subscription {
        val hostId = request.hostId
            ?: throw IllegalArgumentException("EVENT_HOST 구독에는 hostId 가 필요합니다")
        val host = hostRepository.findByIdOrThrow(hostId)
        require(!subscriptionRepository.existsByUserIdAndTypeAndHostId(user.id!!, type, hostId)) {
            "이미 해당 주최를 구독 중입니다"
        }
        return Subscription(user = user, type = type, host = host)
    }

    override fun toResponse(subscription: Subscription): SubscriptionResponse {
        val host = subscription.host!!
        return EventHostSubscriptionResponse(
            id = subscription.id!!,
            host = SubscriptionTargetHost(host.id!!, host.name),
            createdAt = subscription.createdAt,
        )
    }
}
