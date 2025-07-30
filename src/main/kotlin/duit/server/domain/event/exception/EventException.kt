package duit.server.domain.event.exception

import duit.server.domain.common.exception.DomainException
import java.time.LocalDateTime

/**
 * 이벤트 관련 순수 도메인 예외들 (HTTP 의존성 없음)
 */
class EventNotFoundException(val eventId: Long) : 
    DomainException("Event with id $eventId not found")

class EventCapacityExceededException(
    val eventId: Long, 
    val currentCount: Int, 
    val maxCapacity: Int
) : DomainException("Event $eventId capacity exceeded. Current: $currentCount, Max: $maxCapacity")

class EventRegistrationClosedException(
    val eventId: Long, 
    val closedAt: LocalDateTime
) : DomainException("Event $eventId registration is closed since $closedAt")

class EventAlreadyStartedException(
    val eventId: Long, 
    val startTime: LocalDateTime
) : DomainException("Event $eventId already started at $startTime")

class DuplicateEventRegistrationException(
    val eventId: Long, 
    val userId: Long
) : DomainException("User $userId is already registered for event $eventId")

class EventRegistrationNotFoundException(
    val eventId: Long, 
    val userId: Long
) : DomainException("EventRegistration for eventId=$eventId, userId=$userId not found")

class InvalidEventDateException(message: String) : DomainException(message)

class EventCancellationNotAllowedException(
    val eventId: Long, 
    val reason: String
) : DomainException("Event $eventId cannot be cancelled: $reason")

class InvalidEventTypeException(
    val input: String
) : DomainException("Invalid event type: '$input'") {
}