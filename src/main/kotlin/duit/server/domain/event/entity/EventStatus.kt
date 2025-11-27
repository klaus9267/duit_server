package duit.server.domain.event.entity

enum class EventStatus(val description: String, val group: EventStatusGroup) {
    PENDING("승인 대기", EventStatusGroup.PENDING),
    RECRUITMENT_WAITING("모집 대기", EventStatusGroup.ACTIVE),
    RECRUITING("모집 중", EventStatusGroup.ACTIVE),
    EVENT_WAITING("시작 대기", EventStatusGroup.ACTIVE),
    ACTIVE("진행 중", EventStatusGroup.ACTIVE),
    FINISHED("종료", EventStatusGroup.FINISHED)
}
