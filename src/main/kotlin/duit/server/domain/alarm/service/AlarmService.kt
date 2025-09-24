package duit.server.domain.alarm.service

import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.bookmark.repository.BookmarkRepository
import duit.server.domain.event.entity.Event
import duit.server.infrastructure.external.firebase.FCMService
import org.springframework.stereotype.Service

@Service
class AlarmService(
    private val fcmService: FCMService,
    private val bookmarkRepository: BookmarkRepository
) {

    /**
     * 알람 타입과 이벤트를 받아서 알림 전송
     */
    fun sendAlarm(alarmType: AlarmType, event: Event) {
        val eligibleUsers = bookmarkRepository.findEligibleUsersForNotification(event.id!!)

        if (eligibleUsers.isEmpty()) {
            return
        }

        // 2. 알림 내용 생성
        val (title, body, data) = createAlarmContent(alarmType, event)

        // 3. 디바이스 토큰 추출 (이미 필터링되어 있음)
        val deviceTokens = eligibleUsers.map { it.deviceToken!! }

        // 4. FCM 전송
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
                "듀근 듀근 ☺️❤️ [${event.title}]가 내일 시작됩니다! ",
                mapOf(
                    "type" to "event_start",
                    "eventId" to event.id.toString(),
                    "hostName" to event.host.name
                )
            )

            AlarmType.RECRUITMENT_START -> Triple(
                "내일 북마크한 행사의 모집이 시작됩니다",
                "\uD83D\uDCE2[${event.title}]의 모집이 내일 ${event.recruitmentStartAt}시부터 시작됩니다! 잊지말고 신청하세요!",
                mapOf(
                    "type" to "recruitment_start",
                    "eventId" to event.id.toString(),
                    "hostName" to event.host.name
                )
            )

            AlarmType.RECRUITMENT_END -> Triple(
                "내일 북마크한 행사의 모집이 마감됩니다.",
                "⏰[${event.title}]의 모집이 내일 ${event.recruitmentEndAt}시에 마감됩니다. 잊진 않으셨죠?🫨",
                mapOf(
                    "type" to "recruitment_start",
                    "eventId" to event.id.toString(),
                    "hostName" to event.host.name
                )
            )
        }
    }
}