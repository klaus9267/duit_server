package duit.server.application.exception

import duit.server.application.common.ErrorCode
import duit.server.domain.common.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

/**
 * 에러 응답 빌드 유틸리티 - 중복 코드 제거
 */
@Component
class ErrorResponseBuilder {
    
    private val logger = LoggerFactory.getLogger(ErrorResponseBuilder::class.java)
    
    /**
     * 도메인 예외 응답 생성
     */
    fun buildDomainErrorResponse(
        errorCode: ErrorCode,
        exception: Exception,
        request: HttpServletRequest,
        additionalDetails: String? = null
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.warn("Domain exception: ${exception::class.simpleName} - ${exception.message}")
        
        val errorResponse = ErrorResponse(
            code = errorCode.code,
            message = errorCode.message,
            details = additionalDetails ?: exception.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(errorCode.httpStatus).body(errorResponse)
    }
    
    /**
     * 애플리케이션 예외 응답 생성
     */
    fun buildApplicationErrorResponse(
        errorCode: ErrorCode,
        exception: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.error("Application exception: ${exception::class.simpleName} - ${exception.message}", exception)
        
        val errorResponse = ErrorResponse(
            code = errorCode.code,
            message = errorCode.message,
            details = exception.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(errorCode.httpStatus).body(errorResponse)
    }
    
    /**
     * 시스템 예외 응답 생성 (validation, HTTP 관련)
     */
    fun buildSystemErrorResponse(
        errorCode: ErrorCode,
        exception: Exception,
        request: HttpServletRequest,
        details: String? = null
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        if (errorCode.httpStatus.is5xxServerError) {
            logger.error("System error: ${exception::class.simpleName} - ${exception.message}", exception)
        } else {
            logger.warn("Client error: ${exception::class.simpleName} - ${exception.message}")
        }
        
        val errorResponse = ErrorResponse(
            code = errorCode.code,
            message = errorCode.message,
            details = details ?: exception.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(errorCode.httpStatus).body(errorResponse)
    }
    
    /**
     * 추적 ID 생성 (public 메서드)
     */
    fun generateTraceId(): String {
        return MDC.get("traceId") ?: UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }
}
