package duit.server.domain.subscription.entity

import duit.server.domain.alarm.entity.AlarmType

/**
 * 구독 종류 (5종).
 *
 * 새 구독 종류 추가 시:
 *  - [Subscription.init] invariant 분기 추가
 *  - `SubscriptionFactory` 구현체 추가 (Spring 이 자동 주입)
 *  - `SubscriptionResponse` 자식 클래스 추가
 *  - [toAlarmType] when 분기 + [AlarmType] enum 케이스 추가
 */
enum class SubscriptionType {
    EVENT_KEYWORD,    // 행사 키워드 (제목 부분일치)
    EVENT_HOST,       // 행사 주최
    EVENT_TYPE,       // 행사 유형 (CONFERENCE, SEMINAR, ...)
    JOB_KEYWORD,      // 채용 키워드 (제목 부분일치)
    JOB_COMPANY,      // 채용 회사
}

/**
 * 구독 발생 시 만들어질 [AlarmType] 으로의 1:1 매핑.
 * when exhaustive — 새 [SubscriptionType] 추가 시 컴파일 에러로 강제 동기화.
 */
fun SubscriptionType.toAlarmType(): AlarmType = when (this) {
    SubscriptionType.EVENT_KEYWORD -> AlarmType.EVENT_SUBSCRIPTION_KEYWORD
    SubscriptionType.EVENT_HOST -> AlarmType.EVENT_SUBSCRIPTION_HOST
    SubscriptionType.EVENT_TYPE -> AlarmType.EVENT_SUBSCRIPTION_TYPE
    SubscriptionType.JOB_KEYWORD -> AlarmType.JOB_SUBSCRIPTION_KEYWORD
    SubscriptionType.JOB_COMPANY -> AlarmType.JOB_SUBSCRIPTION_COMPANY
}
