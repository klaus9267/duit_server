package duit.server.domain.alarm.entity

enum class AlarmType {
    // 북마크 기반 — 기존
    EVENT_START,                    // 북마크한 행사 시작
    RECRUITMENT_START,              // 모집 시작
    RECRUITMENT_END,                // 모집 종료

    // 구독 기반 — 신규 (Phase 4/5 에서 발송 로직 추가)
    EVENT_SUBSCRIPTION_KEYWORD,     // 키워드 매칭 행사 등록
    EVENT_SUBSCRIPTION_HOST,        // 구독 주최의 행사 등록
    EVENT_SUBSCRIPTION_TYPE,        // 구독 행사 유형의 행사 등록
    JOB_SUBSCRIPTION_KEYWORD,       // 키워드 매칭 채용공고 등록
    JOB_SUBSCRIPTION_COMPANY,       // 구독 회사의 채용공고 등록
}
