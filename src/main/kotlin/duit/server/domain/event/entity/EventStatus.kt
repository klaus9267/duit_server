package duit.server.domain.event.entity

enum class EventStatus(val description: String) {
    PENDING("미승인"),
    SCHEDULED("예정"),
    ONGOING("진행중"),
    FINISHED("종료");
}
