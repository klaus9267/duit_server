package duit.server.domain.subscription.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.subscription.dto.SubscriptionCreateRequest
import duit.server.domain.subscription.dto.SubscriptionResponse
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.repository.SubscriptionRepository
import duit.server.domain.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SubscriptionService(
    private val factories: List<SubscriptionFactory>,
    private val subscriptionRepository: SubscriptionRepository,
    private val userService: UserService,
    private val securityUtil: SecurityUtil,
) {
    private val factoryByType: Map<SubscriptionType, SubscriptionFactory> =
        factories.associateBy { it.type }

    init {
        val missing = SubscriptionType.values().toSet() - factoryByType.keys
        check(missing.isEmpty()) { "SubscriptionFactory 누락: $missing" }
    }

    @Transactional
    fun createSubscription(request: SubscriptionCreateRequest): SubscriptionResponse {
        val user = userService.findUserById(securityUtil.getCurrentUserId())
        val factory = factoryByType.getValue(request.type)
        val saved = subscriptionRepository.save(factory.create(user, request))
        return factory.toResponse(saved)
    }

    fun getSubscriptions(type: SubscriptionType?): List<SubscriptionResponse> {
        val userId = securityUtil.getCurrentUserId()
        val subscriptions = type
            ?.let { subscriptionRepository.findAllByUserIdAndTypeOrderByIdDesc(userId, it) }
            ?: subscriptionRepository.findAllByUserIdOrderByIdDesc(userId)
        return subscriptions.map { factoryByType.getValue(it.type).toResponse(it) }
    }

    @Transactional
    fun deleteSubscription(subscriptionId: Long) {
        val userId = securityUtil.getCurrentUserId()
        val subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
            ?: throw IllegalArgumentException("구독을 찾을 수 없거나 권한이 없습니다")
        subscriptionRepository.delete(subscription)
    }
}
