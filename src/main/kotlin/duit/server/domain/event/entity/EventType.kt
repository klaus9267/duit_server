package duit.server.domain.event.entity

enum class EventType(val displayName: String) {
    CONFERENCE("컨퍼런스/학술대회"),
    SEMINAR("세미나"),
    WEBINAR("웨비나"),
    WORKSHOP("워크숍"),
    CONTEST("공모전"),
    CONTINUING_EDUCATION("보수교육"),
    EDUCATION("교육"),
    VOLUNTEER("봉사"),
    TRAINING("연수"),
    ETC("기타");
}