package duit.server.application.exception.handler

import duit.server.application.common.ErrorCode
import duit.server.application.exception.ErrorResponseBuilder
import duit.server.domain.common.dto.ErrorResponse
import duit.server.domain.view.exception.ViewNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 조회수 도메인 전용 예외 처리 핸들러
 */
@RestControllerAdvice
@Order(1) // 우선순위 1 - 가장 먼저 처리
class ViewExceptionHandler(
    private val errorResponseBuilder: ErrorResponseBuilder
) {

    @ExceptionHandler(ViewNotFoundException::class)
    fun handleViewNotFoundException(
        ex: ViewNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.USER_NOT_FOUND,
            exception = ex,
            request = request,
            additionalDetails = ex.message
        )
    }
}
