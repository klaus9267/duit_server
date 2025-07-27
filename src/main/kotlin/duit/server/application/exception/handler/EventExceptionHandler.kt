package duit.server.application.exception.handler

import duit.server.application.common.ErrorCode
import duit.server.application.controller.dto.ErrorResponse
import duit.server.application.exception.ErrorResponseBuilder
import duit.server.domain.event.exception.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 이벤트 도메인 전용 예외 처리 핸들러 (간소화)
 */
@RestControllerAdvice
@Order(1) // 우선순위 1 - 가장 먼저 처리
class EventExceptionHandler(
    private val errorResponseBuilder: ErrorResponseBuilder
) {
    
    @ExceptionHandler(EventNotFoundException::class)
    fun handleEventNotFoundException(
        ex: EventNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.EVENT_NOT_FOUND,
            exception = ex,
            request = request,
            additionalDetails = "이벤트 ID ${ex.eventId}를 찾을 수 없습니다."
        )
    }
    
    @ExceptionHandler(EventCapacityExceededException::class)
    fun handleEventCapacityExceededException(
        ex: EventCapacityExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.EVENT_CAPACITY_EXCEEDED,
            exception = ex,
            request = request,
            additionalDetails = "이벤트 정원 초과 (현재: ${ex.currentCount}명, 최대: ${ex.maxCapacity}명)"
        )
    }
    
    @ExceptionHandler(EventRegistrationClosedException::class)
    fun handleEventRegistrationClosedException(
        ex: EventRegistrationClosedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.EVENT_REGISTRATION_CLOSED,
            exception = ex,
            request = request,
            additionalDetails = "이벤트 등록이 ${ex.closedAt}에 마감되었습니다."
        )
    }
}
