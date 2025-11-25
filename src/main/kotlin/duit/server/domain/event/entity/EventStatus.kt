package duit.server.domain.event.entity

enum class EventStatus(val description: String) {
    PENDING("승인 대기"),
    ACTIVE("활성"),
    RECRUITING("모집 중"),
    FINISHED("종료")
}
