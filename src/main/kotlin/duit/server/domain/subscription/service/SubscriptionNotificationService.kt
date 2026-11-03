package duit.server.domain.subscription.service

import duit.server.application.config.PushExecutorConfig
import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.repository.AlarmRepository
import duit.server.domain.event.entity.Event
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.entity.toAlarmType
import duit.server.domain.subscription.service.matcher.EventSubscriptionMatcher
import duit.server.domain.subscription.service.matcher.JobSubscriptionMatcher
import duit.server.domain.user.entity.User
import duit.server.infrastructure.external.firebase.FCMService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.Executor

/**
 * 구독 매칭 → 알람 생성 → FCM 발송.
 * 알람 행은 호출한 트랜잭션(승인 등)과 함께 커밋하고, FCM 은 커밋 뒤 푸시 전용 스레드에서 보낸다 (performance D-023):
 *  - 승인 API 가 구독자 수 × FCM 왕복을 기다리지 않고, 그동안 DB 커넥션을 잡고 있지도 않는다.
 *  - 승인이 롤백되면 푸시도 나가지 않는다 (전에는 커밋 전에 보내 롤백돼도 푸시가 나갔다).
 *
 * 호출 지점:
 *  - [duit.server.domain.event.service.EventService] 행사 승인 시 (PENDING → RECRUITMENT_WAITING, autoApprove 포함)
 *  - [duit.server.domain.job.service.JobSyncService] 채용공고 신규 저장 시
 */
@Service
class SubscriptionNotificationService(
    private val eventMatcher: EventSubscriptionMatcher,
    private val jobMatcher: JobSubscriptionMatcher,
    private val alarmRepository: AlarmRepository,
    private val fcmService: FCMService,
    @Qualifier(PushExecutorConfig.PUSH_EXECUTOR) private val pushExecutor: Executor,
) {

    @Transactional
    fun notifyOnEventApproved(event: Event) {
        eventMatcher.findMatchedSubscriptions(event).forEach { subscription ->
            val alarmType = subscription.type.toAlarmType()
            if (alarmRepository.existsByUserIdAndEventIdAndType(subscription.user.id!!, event.id!!, alarmType)) {
                return@forEach
            }
            try {
                alarmRepository.save(Alarm(user = subscription.user, event = event, type = alarmType))
            } catch (_: DataIntegrityViolationException) {
                return@forEach
            }
            val (title, body, data) = buildEventContent(subscription, event)
            sendFcmAfterCommit(subscription.user, title, body, data)
        }
    }

    @Transactional
    fun notifyOnJobPostingCreated(jobPosting: JobPosting) {
        jobMatcher.findMatchedSubscriptions(jobPosting).forEach { subscription ->
            val alarmType = subscription.type.toAlarmType()
            if (alarmRepository.existsByUserIdAndJobPostingIdAndType(subscription.user.id!!, jobPosting.id!!, alarmType)) {
                return@forEach
            }
            try {
                alarmRepository.save(Alarm(user = subscription.user, jobPosting = jobPosting, type = alarmType))
            } catch (_: DataIntegrityViolationException) {
                return@forEach
            }
            val (title, body, data) = buildJobContent(subscription, jobPosting)
            sendFcmAfterCommit(subscription.user, title, body, data)
        }
    }

    private fun buildEventContent(
        subscription: Subscription,
        event: Event,
    ): Triple<String, String, Map<String, String>> {
        val baseData = mapOf(
            "eventId" to event.id!!.toString(),
            "subscriptionId" to subscription.id!!.toString(),
        )
        return when (subscription.type) {
            SubscriptionType.EVENT_KEYWORD -> Triple(
                "구독한 키워드의 새 행사 🔔",
                "'${subscription.keyword}' 키워드와 일치하는 새 행사: ${event.title}",
                baseData + ("type" to "event_subscription_keyword"),
            )
            SubscriptionType.EVENT_HOST -> Triple(
                "구독한 주최의 새 행사 🔔",
                "${subscription.host?.name ?: "구독 주최"}에서 새 행사를 등록했어요: ${event.title}",
                baseData + ("type" to "event_subscription_host"),
            )
            SubscriptionType.EVENT_TYPE -> Triple(
                "구독한 유형의 새 행사 🔔",
                "새 ${event.eventType} 행사가 등록됐어요: ${event.title}",
                baseData + ("type" to "event_subscription_type"),
            )
            SubscriptionType.JOB_KEYWORD,
            SubscriptionType.JOB_COMPANY ->
                error("Event 알림에 잘못된 SubscriptionType: ${subscription.type}")
        }
    }

    private fun buildJobContent(
        subscription: Subscription,
        jobPosting: JobPosting,
    ): Triple<String, String, Map<String, String>> {
        val title = jobPosting.wantedTitle ?: "채용공고"
        val baseData = mapOf(
            "jobPostingId" to jobPosting.id!!.toString(),
            "subscriptionId" to subscription.id!!.toString(),
        )
        return when (subscription.type) {
            SubscriptionType.JOB_KEYWORD -> Triple(
                "구독한 키워드의 새 채용공고 🔔",
                "'${subscription.keyword}' 키워드와 일치하는 새 채용공고: $title",
                baseData + ("type" to "job_subscription_keyword"),
            )
            SubscriptionType.JOB_COMPANY -> Triple(
                "구독한 회사의 새 채용공고 🔔",
                "${subscription.company?.corpNm ?: "구독 회사"}에서 새 채용공고를 올렸어요: $title",
                baseData + ("type" to "job_subscription_company"),
            )
            SubscriptionType.EVENT_KEYWORD,
            SubscriptionType.EVENT_HOST,
            SubscriptionType.EVENT_TYPE ->
                error("Job 알림에 잘못된 SubscriptionType: ${subscription.type}")
        }
    }

    private fun sendFcmAfterCommit(user: User, title: String, body: String, data: Map<String, String>) {
        val tokens = user.deviceTokens.map { it.token } // 지연 로딩이라 트랜잭션 안에서 읽어 둔다
        if (tokens.isEmpty()) return
        val send = Runnable { fcmService.sendAlarms(deviceTokens = tokens, title = title, body = body, data = data) }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            pushExecutor.execute(send)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = pushExecutor.execute(send)
        })
    }
}
