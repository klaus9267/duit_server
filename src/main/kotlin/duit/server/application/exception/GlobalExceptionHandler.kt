package duit.server.application.exception

import duit.server.application.common.ErrorCode
import duit.server.application.dto.ErrorResponse
import duit.server.application.dto.FieldError
import duit.server.domain.common.exception.DomainException
import duit.server.domain.user.exception.*
import duit.server.domain.event.exception.*
import duit.server.application.exception.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.*

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    // ===== 🎯 도메인 예외들 - 타입별 개별 핸들링 =====
    
    /**
     * 사용자 조회 실패 예외
     */
    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(
        ex: UserNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        logger.warn("User not found: userId=${ex.userId}")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.USER_NOT_FOUND.code,
            message = ErrorCode.USER_NOT_FOUND.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(ErrorCode.USER_NOT_FOUND.httpStatus).body(errorResponse)
    }
    
    /**
     * 이메일로 사용자 조회 실패 예외
     */
    @ExceptionHandler(UserEmailNotFoundException::class)
    fun handleUserEmailNotFoundException(
        ex: UserEmailNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        logger.warn("User not found by email: ${ex.email}")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.USER_EMAIL_NOT_FOUND.code,
            message = ErrorCode.USER_EMAIL_NOT_FOUND.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(ErrorCode.USER_EMAIL_NOT_FOUND.httpStatus).body(errorResponse)
    }
    
    /**
     * 중복 이메일 예외
     */
    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmailException(
        ex: DuplicateEmailException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        logger.warn("Duplicate email attempted: ${ex.email}")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.DUPLICATE_EMAIL.code,
            message = ErrorCode.DUPLICATE_EMAIL.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(ErrorCode.DUPLICATE_EMAIL.httpStatus).body(errorResponse)
    }
    
    /**
     * 이벤트 조회 실패 예외
     */
    @ExceptionHandler(EventNotFoundException::class)
    fun handleEventNotFoundException(
        ex: EventNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        logger.warn("Event not found: eventId=${ex.eventId}")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.EVENT_NOT_FOUND.code,
            message = ErrorCode.EVENT_NOT_FOUND.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(ErrorCode.EVENT_NOT_FOUND.httpStatus).body(errorResponse)
    }
    
    /**
     * 이벤트 정원 초과 예외
     */
    @ExceptionHandler(EventCapacityExceededException::class)
    fun handleEventCapacityExceededException(
        ex: EventCapacityExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        logger.warn("Event capacity exceeded: eventId=${ex.eventId}, current=${ex.currentCount}, max=${ex.maxCapacity}")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.EVENT_CAPACITY_EXCEEDED.code,
            message = ErrorCode.EVENT_CAPACITY_EXCEEDED.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(ErrorCode.EVENT_CAPACITY_EXCEEDED.httpStatus).body(errorResponse)
    }
    
    /**
     * 이벤트 등록 마감 예외
     */
    @ExceptionHandler(EventRegistrationClosedException::class)
    fun handleEventRegistrationClosedException(
        ex: EventRegistrationClosedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        logger.warn("Event registration closed: eventId=${ex.eventId}, closedAt=${ex.closedAt}")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.EVENT_REGISTRATION_CLOSED.code,
            message = ErrorCode.EVENT_REGISTRATION_CLOSED.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(ErrorCode.EVENT_REGISTRATION_CLOSED.httpStatus).body(errorResponse)
    }
    
    /**
     * 외부 서비스 예외
     */
    @ExceptionHandler(ExternalServiceException::class)
    fun handleExternalServiceException(
        ex: ExternalServiceException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        logger.error("External service error: service=${ex.serviceName}, message=${ex.message}", ex)
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.EXTERNAL_SERVICE_ERROR.code,
            message = ErrorCode.EXTERNAL_SERVICE_ERROR.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(ErrorCode.EXTERNAL_SERVICE_ERROR.httpStatus).body(errorResponse)
    }
    
    // ===== 🎯 일반적인 도메인/애플리케이션 예외 fallback =====
    
    /**
     * 도메인 예외 처리 (구체적인 핸들러가 없는 경우)
     */
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        ex: DomainException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.warn("Domain exception occurred: ${ex.message}", ex)
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.CONFLICT.code,
            message = ErrorCode.CONFLICT.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(ErrorCode.CONFLICT.httpStatus).body(errorResponse)
    }
    
    /**
     * 애플리케이션 예외 처리 (구체적인 핸들러가 없는 경우)
     */
    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(
        ex: ApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.error("Application exception occurred: ${ex.message}", ex)
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = ErrorCode.INTERNAL_SERVER_ERROR.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus).body(errorResponse)
    }
    
    /**
     * 검증 예외 처리 (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        val fieldErrors = ex.bindingResult.fieldErrors.map { fieldError ->
            FieldError(
                field = fieldError.field,
                rejectedValue = fieldError.rejectedValue,
                message = fieldError.defaultMessage ?: "Invalid value"
            )
        }
        
        logger.warn("Validation failed: ${fieldErrors.size} errors")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.VALIDATION_FAILED.code,
            message = ErrorCode.VALIDATION_FAILED.message,
            fieldErrors = fieldErrors,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    /**
     * Bind 예외 처리
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        ex: BindException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        val fieldErrors = ex.fieldErrors.map { fieldError ->
            FieldError(
                field = fieldError.field,
                rejectedValue = fieldError.rejectedValue,
                message = fieldError.defaultMessage ?: "Invalid value"
            )
        }
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.VALIDATION_FAILED.code,
            message = ErrorCode.VALIDATION_FAILED.message,
            fieldErrors = fieldErrors,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    /**
     * Constraint 위반 예외 처리
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        val fieldErrors = ex.constraintViolations.map { violation ->
            FieldError(
                field = violation.propertyPath.toString(),
                rejectedValue = violation.invalidValue,
                message = violation.message
            )
        }
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.VALIDATION_FAILED.code,
            message = ErrorCode.VALIDATION_FAILED.message,
            fieldErrors = fieldErrors,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    /**
     * 인증 예외 처리
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.warn("Authentication failed: ${ex.message}")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.UNAUTHORIZED.code,
            message = ErrorCode.UNAUTHORIZED.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    /**
     * 접근 권한 예외 처리
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.warn("Access denied: ${ex.message}")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.FORBIDDEN.code,
            message = ErrorCode.FORBIDDEN.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }
    
    /**
     * HTTP 메서드 미지원 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupportedException(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.METHOD_NOT_ALLOWED.code,
            message = ErrorCode.METHOD_NOT_ALLOWED.message,
            details = "Supported methods: ${ex.supportedHttpMethods?.joinToString(", ")}",
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse)
    }
    
    /**
     * 핸들러를 찾을 수 없는 예외 처리
     */
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(
        ex: NoHandlerFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.NOT_FOUND.code,
            message = ErrorCode.NOT_FOUND.message,
            details = "No handler found for ${ex.httpMethod} ${ex.requestURL}",
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
    
    /**
     * 잘못된 파라미터 타입 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.INVALID_REQUEST.code,
            message = ErrorCode.INVALID_REQUEST.message,
            details = "Parameter '${ex.name}' should be of type ${ex.requiredType?.simpleName}",
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    /**
     * 필수 파라미터 누락 예외 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameterException(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.INVALID_REQUEST.code,
            message = ErrorCode.INVALID_REQUEST.message,
            details = "Required parameter '${ex.parameterName}' is missing",
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    /**
     * HTTP 메시지 읽기 불가 예외 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.INVALID_REQUEST.code,
            message = ErrorCode.INVALID_REQUEST.message,
            details = "Malformed JSON request",
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    /**
     * 데이터베이스 예외 처리
     */
    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccessException(
        ex: DataAccessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.error("Database access error: ${ex.message}", ex)
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.DATA_ACCESS_ERROR.code,
            message = ErrorCode.DATA_ACCESS_ERROR.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
    
    /**
     * 데이터 무결성 위반 예외 처리
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.error("Data integrity violation: ${ex.message}", ex)
        
        // 중복 키 에러인지 확인
        val isDuplicateKey = ex.cause?.message?.contains("Duplicate") == true ||
                ex.cause?.message?.contains("duplicate") == true ||
                ex.cause?.message?.contains("UNIQUE") == true
        
        val errorCode = if (isDuplicateKey) ErrorCode.CONFLICT else ErrorCode.DATA_ACCESS_ERROR
        
        val errorResponse = ErrorResponse(
            code = errorCode.code,
            message = errorCode.message,
            details = if (isDuplicateKey) "Duplicate data detected" else null,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(errorCode.httpStatus).body(errorResponse)
    }
    
    /**
     * SQL 예외 처리
     */
    @ExceptionHandler(SQLException::class)
    fun handleSQLException(
        ex: SQLException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.error("SQL error occurred: ${ex.message}", ex)
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.DATA_ACCESS_ERROR.code,
            message = ErrorCode.DATA_ACCESS_ERROR.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
    
    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.warn("Illegal argument: ${ex.message}")
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.INVALID_REQUEST.code,
            message = ErrorCode.INVALID_REQUEST.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    /**
     * IllegalStateException 처리
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.error("Illegal state: ${ex.message}", ex)
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = ErrorCode.INTERNAL_SERVER_ERROR.message,
            details = ex.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
    
    /**
     * NullPointerException 처리
     */
    @ExceptionHandler(NullPointerException::class)
    fun handleNullPointerException(
        ex: NullPointerException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.error("Null pointer exception occurred", ex)
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = ErrorCode.INTERNAL_SERVER_ERROR.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
    
    /**
     * 모든 기타 예외 처리 (최종 fallback)
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val traceId = generateTraceId()
        
        logger.error("Unexpected error occurred: ${ex.message}", ex)
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = ErrorCode.INTERNAL_SERVER_ERROR.message,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = traceId
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
    
    /**
     * 추적 ID 생성
     */
    private fun generateTraceId(): String {
        return MDC.get("traceId") ?: UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }
}
