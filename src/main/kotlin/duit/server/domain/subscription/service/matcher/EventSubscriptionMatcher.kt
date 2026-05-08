package duit.server.domain.subscription.service.matcher

import duit.server.domain.event.entity.Event
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.repository.SubscriptionRepository
import org.springframework.stereotype.Component

/**
 * 행사 등록/승인 시 매칭되는 구독을 찾는다.
 * EVENT_KEYWORD (제목 부분일치), EVENT_HOST, EVENT_TYPE 3종 합집합.
 */
@Component
class EventSubscriptionMatcher(
    private val subscriptionRepository: SubscriptionRepository,
) {

    fun findMatchedSubscriptions(event: Event): List<Subscription> {
        val keywordMatches = subscriptionRepository
            .findAllByType(SubscriptionType.EVENT_KEYWORD)
            .filter { event.title.contains(it.keyword!!, ignoreCase = true) }

        val hostMatches = event.host.id?.let { hostId ->
            subscriptionRepository.findAllByTypeAndHostId(SubscriptionType.EVENT_HOST, hostId)
        } ?: emptyList()

        val typeMatches = subscriptionRepository
            .findAllByTypeAndEventType(SubscriptionType.EVENT_TYPE, event.eventType)

        return keywordMatches + hostMatches + typeMatches
    }
}
