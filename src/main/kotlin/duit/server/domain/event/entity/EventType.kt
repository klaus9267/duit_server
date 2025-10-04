package duit.server.domain.event.entity

enum class EventType(val displayName: String) {
    CONFERENCE("컨퍼런스/학술대회"),
    SEMINAR("세미나"),
    WEBINAR("웨비나"),
    WORKSHOP("워크숍"),
    CONTEST("공모전"),
    CONTINUING_EDUCATION("보수교육"),
    ETC("기타");

    companion object {
        fun of(type: String): EventType {
            if (type.isBlank()) {
                throw IllegalArgumentException("이벤트 타입이 비어있습니다: $type")
            }

            return entries.find { it.displayName == type } ?: EventType.ETC
        }
    }
}