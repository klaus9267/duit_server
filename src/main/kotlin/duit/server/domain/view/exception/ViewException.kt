package duit.server.domain.view.exception

import duit.server.domain.common.exception.DomainException

/**
 * 조회수 관련 순수 도메인 예외들 (HTTP 의존성 없음)
 */
class ViewNotFoundException(val eventId: Long) :
    DomainException("View with eventId $eventId not found")
