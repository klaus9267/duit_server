package duit.server.domain.event.entity

enum class EventStatusGroup(val description: String) {
    PENDING("승인 대기"),
    ACTIVE("활성"),
    FINISHED("종료")
}
