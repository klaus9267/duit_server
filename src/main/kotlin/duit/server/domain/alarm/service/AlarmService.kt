package duit.server.domain.alarm.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.alarm.dto.AlarmPaginationParam
import duit.server.domain.alarm.dto.AlarmResponse
import duit.server.domain.alarm.dto.AlarmResponseV2
import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.repository.AlarmRepository
import duit.server.domain.bookmark.repository.BookmarkRepository
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.event.entity.Event
import duit.server.domain.event.repository.EventRepository
import duit.server.infrastructure.external.firebase.FCMService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AlarmService(
    private val fcmService: FCMService,
    private val eventRepository: EventRepository,
    private val alarmRepository: AlarmRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val securityUtil: SecurityUtil,
) {

    /**
     * 알람 목록 조회 (V1) — 이벤트 알람만 반환.
     *
     * 채용/구독 알람(event IS NULL) 은 V2 엔드포인트(폴리모픽 응답)에서 노출.
     * 기존 V1 클라이언트의 응답 스키마가 event 필드를 non-null 로 가정하기 때문에 필터링.
     */
    fun getAlarms(param: AlarmPaginationParam): PageResponse<AlarmResponse> {
        val currentUserId = securityUtil.getCurrentUserId()
        val alarms = if (param.isRead != null) {
            alarmRepository.findByUserIdAndEventIsNotNullAndIsRead(currentUserId, param.isRead, param.toPageable())
        } else {
            alarmRepository.findByUserIdAndEventIsNotNull(currentUserId, param.toPageable())
        }
        val alarmResponses = alarms.content.map { AlarmResponse.from(it) }

        return PageResponse(
            content = alarmResponses,
            pageInfo = PageInfo.from(alarms)
        )
    }

    /**
     * 알람 목록 조회 (V2) — 이벤트 + 채용 알람 모두 폴리모픽 응답으로 반환.
     */
    fun getAlarmsV2(param: AlarmPaginationParam): PageResponse<AlarmResponseV2> {
        val currentUserId = securityUtil.getCurrentUserId()
        val alarms = if (param.isRead != null) {
            alarmRepository.findByUserIdAndIsRead(currentUserId, param.isRead, param.toPageable())
        } else {
            alarmRepository.findByUserId(currentUserId, param.toPageable())
        }
        return PageResponse(
            content = alarms.content.map { AlarmResponseV2.from(it) },
            pageInfo = PageInfo.from(alarms),
        )
    }

    /**
     * 단일 알람 읽음 처리
     */
    @Transactional
    fun markAsRead(alarmId: Long) {
        val currentUserId = securityUtil.getCurrentUserId()
        val alarm = alarmRepository.findByUserIdAndId(currentUserId, alarmId)
            ?: throw IllegalArgumentException("알람을 찾을 수 없거나 권한이 없습니다")

        alarm.isRead = true
        alarmRepository.save(alarm)
    }

    /**
     * 전체 알람 읽음 처리
     */
    @Transactional
    fun markAllAsRead() {
        val currentUserId = securityUtil.getCurrentUserId()
        val alarms = alarmRepository.findByUserId(currentUserId, PageRequest.of(0, Int.MAX_VALUE))

        alarms.content.forEach { it.isRead = true }
        alarmRepository.saveAll(alarms.content)
    }

    /**
     * 단일 알람 삭제
     */
    @Transactional
    fun deleteAlarm(alarmId: Long) {
        val currentUserId = securityUtil.getCurrentUserId()
        val alarm = alarmRepository.findByUserIdAndId(currentUserId, alarmId)
            ?: throw IllegalArgumentException("알람을 찾을 수 없거나 권한이 없습니다")

        alarmRepository.delete(alarm)
    }

    /**
     * 알람 전체 삭제 (readOnly 파라미터로 읽은 것만/전체 선택)
     */
    @Transactional
    fun deleteAlarms(readOnly: Boolean) {
        val currentUserId = securityUtil.getCurrentUserId()

        if (readOnly) {
            alarmRepository.deleteByUserIdAndIsRead(currentUserId, true)
        } else {
            alarmRepository.deleteByUserId(currentUserId)
        }
    }

    /**
     * 알람 생성 (스케줄러에서 호출)
     */
    @Transactional
    fun createAlarms(alarmType: AlarmType, eventId: Long) {
        val event = eventRepository.findByIdOrNull(eventId) ?: return
        val eligibleUsers = bookmarkRepository.findEligibleUsersForAlarms(event.id!!).distinctBy { it.id }
        if (eligibleUsers.isEmpty()) {
            return
        }

        val newAlarmUsers = eligibleUsers.mapNotNull { user ->
            if (alarmRepository.existsByUserIdAndEventIdAndType(user.id!!, event.id!!, alarmType)) {
                return@mapNotNull null
            }

            try {
                val alarm = Alarm(
                    user = user,
                    event = event,
                    type = alarmType,
                )
                alarmRepository.save(alarm)
                user
            } catch (_: DataIntegrityViolationException) {
                null
            }
        }

        if (newAlarmUsers.isEmpty()) return

        val deviceTokens = newAlarmUsers.flatMap { user ->
            user.deviceTokens.map { it.token }
        }
        if (deviceTokens.isEmpty()) return

        val (title, body, data) = createAlarmContent(alarmType, event)

        fcmService.sendAlarms(
            deviceTokens = deviceTokens,
            title = title,
            body = body,
            data = data
        )
    }

    /**
     * 알림 타입에 따른 내용 생성
     */
    private fun createAlarmContent(
        alarmType: AlarmType,
        event: Event
    ): Triple<String, String, Map<String, String>> {
        return when (alarmType) {
            AlarmType.EVENT_START -> Triple(
                "내일 북마크한 행사가 시작됩니다 ",
                "듀근 듀근 ☺️❤️ [${event.title}]가 내일 ${event.startAt.hour}시에 시작됩니다! ",
                mapOf(
                    "type" to "event_start",
                    "eventId" to event.id.toString(),
                    "hostName" to event.host.name
                )
            )

            AlarmType.RECRUITMENT_START -> Triple(
                "내일 북마크한 행사의 모집이 시작됩니다",
                "\uD83D\uDCE2[${event.title}]의 모집이 내일 ${event.recruitmentStartAt!!.hour}시부터 시작됩니다! 잊지말고 신청하세요!",
                mapOf(
                    "type" to "recruitment_start",
                    "eventId" to event.id.toString(),
                    "hostName" to event.host.name
                )
            )

            AlarmType.RECRUITMENT_END -> Triple(
                "내일 북마크한 행사의 모집이 마감됩니다.",
                "⏰[${event.title}]의 모집이 내일 ${event.recruitmentEndAt!!.hour}시에 마감됩니다. 잊진 않으셨죠?🫨",
                mapOf(
                    "type" to "recruitment_end",
                    "eventId" to event.id.toString(),
                    "hostName" to event.host.name
                )
            )

            // 구독 기반 알람 — 본 메서드(북마크 스케줄러 흐름)에서는 호출되지 않음.
            // Phase 4/5 의 SubscriptionNotificationService 가 별도로 본문 생성.
            AlarmType.EVENT_SUBSCRIPTION_KEYWORD,
            AlarmType.EVENT_SUBSCRIPTION_HOST,
            AlarmType.EVENT_SUBSCRIPTION_TYPE,
            AlarmType.JOB_SUBSCRIPTION_KEYWORD,
            AlarmType.JOB_SUBSCRIPTION_COMPANY ->
                error("구독 알람($alarmType) 은 AlarmService.createAlarmContent 가 처리하지 않습니다 — SubscriptionNotificationService 사용")
        }
    }
}
