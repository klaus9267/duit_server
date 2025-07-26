package duit.server.application.exception

import duit.server.application.common.ErrorCode
import duit.server.application.dto.ErrorResponse
import duit.server.application.dto.FieldError
import duit.server.domain.common.exception.DomainException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.core.annotation.Order
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
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

/**
 * 전역 예외 처리 핸들러 - 도메인별 핸들러에서 처리하지 않은 모든 예외 처리
 */
@RestControllerAdvice
@Order(10) // 우선순위 10 - 도메인별 핸들러 이후 fallback 역할
class GlobalExceptionHandler(
    private val errorResponseBuilder: ErrorResponseBuilder
) {
    
    // ===== 🎯 도메인/애플리케이션 예외 fallback =====
    
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        ex: DomainException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.CONFLICT,
            exception = ex,
            request = request
        )
    }
    
    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(
        ex: ApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildApplicationErrorResponse(
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR,
            exception = ex,
            request = request
        )
    }
    
    // ===== 🎯 HTTP/Validation 예외들 =====
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map { fieldError ->
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
            traceId = errorResponseBuilder.generateTraceId()
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    @ExceptionHandler(BindException::class, ConstraintViolationException::class)
    fun handleBindingException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = when (ex) {
            is BindException -> ex.fieldErrors.map { fieldError ->
                FieldError(
                    field = fieldError.field,
                    rejectedValue = fieldError.rejectedValue,
                    message = fieldError.defaultMessage ?: "Invalid value"
                )
            }
            is ConstraintViolationException -> ex.constraintViolations.map { violation ->
                FieldError(
                    field = violation.propertyPath.toString(),
                    rejectedValue = violation.invalidValue,
                    message = violation.message
                )
            }
            else -> emptyList()
        }
        
        val errorResponse = ErrorResponse(
            code = ErrorCode.VALIDATION_FAILED.code,
            message = ErrorCode.VALIDATION_FAILED.message,
            fieldErrors = fieldErrors,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
            traceId = errorResponseBuilder.generateTraceId()
        )
        
        return ResponseEntity.badRequest().body(errorResponse)
    }
    
    // ===== 🎯 Spring Security 예외들 =====
    
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildSystemErrorResponse(
            errorCode = ErrorCode.UNAUTHORIZED,
            exception = ex,
            request = request
        )
    }
    
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildSystemErrorResponse(
            errorCode = ErrorCode.FORBIDDEN,
            exception = ex,
            request = request
        )
    }
    
    // ===== 🎯 HTTP 관련 예외들 =====
    
    @ExceptionHandler(
        HttpRequestMethodNotSupportedException::class,
        NoHandlerFoundException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        HttpMessageNotReadableException::class
    )
    fun handleHttpException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val (errorCode, details) = when (ex) {
            is HttpRequestMethodNotSupportedException -> 
                ErrorCode.METHOD_NOT_ALLOWED to "Supported methods: ${ex.supportedHttpMethods?.joinToString(", ")}"
            is NoHandlerFoundException -> 
                ErrorCode.NOT_FOUND to "No handler found for ${ex.httpMethod} ${ex.requestURL}"
            is MethodArgumentTypeMismatchException -> 
                ErrorCode.INVALID_REQUEST to "Parameter '${ex.name}' should be of type ${ex.requiredType?.simpleName}"
            is MissingServletRequestParameterException -> 
                ErrorCode.INVALID_REQUEST to "Required parameter '${ex.parameterName}' is missing"
            is HttpMessageNotReadableException -> 
                ErrorCode.INVALID_REQUEST to "Malformed JSON request"
            else -> ErrorCode.INVALID_REQUEST to null
        }
        
        return errorResponseBuilder.buildSystemErrorResponse(
            errorCode = errorCode,
            exception = ex,
            request = request,
            details = details
        )
    }
    
    // ===== 🎯 데이터베이스 예외들 =====
    
    @ExceptionHandler(DataAccessException::class, SQLException::class)
    fun handleDataAccessException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildSystemErrorResponse(
            errorCode = ErrorCode.DATA_ACCESS_ERROR,
            exception = ex,
            request = request
        )
    }
    
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        // 중복 키 에러인지 확인
        val isDuplicateKey = ex.cause?.message?.let { message ->
            message.contains("Duplicate", ignoreCase = true) ||
            message.contains("duplicate", ignoreCase = true) ||
            message.contains("UNIQUE", ignoreCase = true)
        } ?: false
        
        val errorCode = if (isDuplicateKey) ErrorCode.CONFLICT else ErrorCode.DATA_ACCESS_ERROR
        val details = if (isDuplicateKey) "Duplicate data detected" else null
        
        return errorResponseBuilder.buildSystemErrorResponse(
            errorCode = errorCode,
            exception = ex,
            request = request,
            details = details
        )
    }
    
    // ===== 🎯 일반적인 Java 예외들 =====
    
    @ExceptionHandler(
        IllegalArgumentException::class,
        IllegalStateException::class,
        NullPointerException::class,
        Exception::class
    )
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorCode = when (ex) {
            is IllegalArgumentException -> ErrorCode.INVALID_REQUEST
            else -> ErrorCode.INTERNAL_SERVER_ERROR
        }
        
        return errorResponseBuilder.buildSystemErrorResponse(
            errorCode = errorCode,
            exception = ex,
            request = request
        )
    }
}
