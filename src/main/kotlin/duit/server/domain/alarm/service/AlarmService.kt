package duit.server.domain.alarm.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.alarm.dto.AlarmPaginationParam
import duit.server.domain.alarm.dto.AlarmResponse
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
     * 알람 목록 조회 (페이징, isRead 필터링 지원)
     */
    fun getAlarms(param: AlarmPaginationParam): PageResponse<AlarmResponse> {
        val currentUserId = securityUtil.getCurrentUserId()
        val alarms = if (param.isRead != null) {
            alarmRepository.findByUserIdAndIsRead(currentUserId, param.isRead, param.toPageable())
        } else {
            alarmRepository.findByUserId(currentUserId, param.toPageable())
        }
        val alarmResponses = alarms.content.map { AlarmResponse.from(it) }

        return PageResponse(
            content = alarmResponses,
            pageInfo = PageInfo.from(alarms)
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
        }
    }
}
