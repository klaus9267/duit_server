package duit.server.domain.subscription.entity

/**
 * 구독 종류 (5종).
 *
 * 새 구독 종류 추가 시:
 * 1) 여기에 enum 케이스 추가
 * 2) [Subscription.init] invariant 분기 추가
 * 3) `SubscriptionFactory` 구현체 추가
 * 4) (필요시) `EventSubscriptionMatcher` 또는 `JobSubscriptionMatcher` 구현체 추가
 */
enum class SubscriptionType {
    EVENT_KEYWORD,    // 행사 키워드 (제목 부분일치)
    EVENT_HOST,       // 행사 주최
    EVENT_TYPE,       // 행사 유형 (CONFERENCE, SEMINAR, ...)
    JOB_KEYWORD,      // 채용 키워드 (제목 부분일치)
    JOB_COMPANY,      // 채용 회사
}
