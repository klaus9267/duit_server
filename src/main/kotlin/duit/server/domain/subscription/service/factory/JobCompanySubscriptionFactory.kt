package duit.server.domain.subscription.service.factory

import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.job.repository.JobCompanyRepository
import duit.server.domain.subscription.dto.JobCompanySubscriptionResponse
import duit.server.domain.subscription.dto.SubscriptionCreateRequest
import duit.server.domain.subscription.dto.SubscriptionResponse
import duit.server.domain.subscription.dto.SubscriptionTargetCompany
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.repository.SubscriptionRepository
import duit.server.domain.subscription.service.SubscriptionFactory
import duit.server.domain.user.entity.User
import org.springframework.stereotype.Component

@Component
class JobCompanySubscriptionFactory(
    private val jobCompanyRepository: JobCompanyRepository,
    private val subscriptionRepository: SubscriptionRepository,
) : SubscriptionFactory {

    override val type = SubscriptionType.JOB_COMPANY

    override fun create(user: User, request: SubscriptionCreateRequest): Subscription {
        val companyId = request.companyId
            ?: throw IllegalArgumentException("JOB_COMPANY 구독에는 companyId 가 필요합니다")
        val company = jobCompanyRepository.findByIdOrThrow(companyId)
        require(!subscriptionRepository.existsByUserIdAndTypeAndCompanyId(user.id!!, type, companyId)) {
            "이미 해당 회사를 구독 중입니다"
        }
        return Subscription(user = user, type = type, company = company)
    }

    override fun toResponse(subscription: Subscription): SubscriptionResponse {
        val company = subscription.company!!
        return JobCompanySubscriptionResponse(
            id = subscription.id!!,
            company = SubscriptionTargetCompany(company.id!!, company.corpNm),
            createdAt = subscription.createdAt,
        )
    }
}
