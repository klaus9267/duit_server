package duit.server.domain.alarm.dto

/**
 * 알람이 가리키는 대상 종류.
 *
 * AlarmType (사유) 와는 별개의 직교 개념:
 *  - AlarmType  = 왜 알림이 떴나 (EVENT_START, JOB_SUBSCRIPTION_COMPANY, ...)
 *  - AlarmTargetType = 무엇을 가리키나 (EVENT, JOB_POSTING)
 *
 * 새 대상 종류 추가 시 [AlarmTargetResponse] 의 sealed 구현체도 함께 추가.
 */
enum class AlarmTargetType {
    EVENT,
    JOB_POSTING,
}
