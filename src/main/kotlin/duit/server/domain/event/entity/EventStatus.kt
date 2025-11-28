package duit.server.domain.event.entity

enum class EventStatus(
    val description: String,
    val nextStatus: EventStatus?
) {
    FINISHED("종료", null),
    ACTIVE("진행 중", FINISHED),
    EVENT_WAITING("시작 대기", ACTIVE),
    RECRUITING("모집 중", EVENT_WAITING),
    RECRUITMENT_WAITING("모집 대기", RECRUITING),
    PENDING("승인 대기", null);

    companion object {
        fun getTransitionableStatuses(): List<EventStatus> =
            EventStatus.entries.filter {  it.nextStatus != null }
    }
}
