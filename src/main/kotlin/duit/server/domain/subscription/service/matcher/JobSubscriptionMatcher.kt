package duit.server.domain.subscription.service.matcher

import duit.server.domain.job.entity.JobPosting
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.repository.SubscriptionRepository
import org.springframework.stereotype.Component

/**
 * 채용공고 수집 시 매칭되는 구독을 찾는다.
 * JOB_KEYWORD (제목 부분일치), JOB_COMPANY 2종 합집합.
 */
@Component
class JobSubscriptionMatcher(
    private val subscriptionRepository: SubscriptionRepository,
) {

    fun findMatchedSubscriptions(jobPosting: JobPosting): List<Subscription> {
        val title = jobPosting.wantedTitle?.takeIf { it.isNotBlank() }

        val keywordMatches = if (title != null) {
            subscriptionRepository
                .findAllByType(SubscriptionType.JOB_KEYWORD)
                .filter { title.contains(it.keyword!!, ignoreCase = true) }
        } else emptyList()

        val companyMatches = jobPosting.company?.id?.let { companyId ->
            subscriptionRepository.findAllByTypeAndCompanyId(SubscriptionType.JOB_COMPANY, companyId)
        } ?: emptyList()

        return keywordMatches + companyMatches
    }
}
