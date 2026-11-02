package duit.server.domain.subscription.service.matcher

import duit.server.domain.event.entity.Event
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.repository.SubscriptionRepository
import org.springframework.stereotype.Component

/**
 * 행사 등록/승인 시 매칭되는 구독을 찾는다.
 * EVENT_KEYWORD (제목 부분일치), EVENT_HOST (행사의 주최 중 하나라도 일치), EVENT_TYPE 3종 합집합.
 * 같은 행사의 여러 주최를 구독한 사용자는 매칭이 여러 건이지만 알람은 (user, event, type) 기준으로 1건만 생성된다.
 */
@Component
class EventSubscriptionMatcher(
    private val subscriptionRepository: SubscriptionRepository,
) {

    fun findMatchedSubscriptions(event: Event): List<Subscription> {
        val keywordMatches = subscriptionRepository
            .findAllByType(SubscriptionType.EVENT_KEYWORD)
            .filter { event.title.contains(it.keyword!!, ignoreCase = true) }

        val hostMatches = event.hosts.mapNotNull { it.id }
            .takeIf { it.isNotEmpty() }
            ?.let { hostIds -> subscriptionRepository.findAllByTypeAndHostIdIn(SubscriptionType.EVENT_HOST, hostIds) }
            .orEmpty()

        val typeMatches = subscriptionRepository
            .findAllByTypeAndEventType(SubscriptionType.EVENT_TYPE, event.eventType)

        return keywordMatches + hostMatches + typeMatches
    }
}
