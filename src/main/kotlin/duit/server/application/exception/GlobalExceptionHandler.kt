package duit.server.application.exception

import duit.server.application.common.ErrorCode
import duit.server.domain.common.dto.ErrorResponse
import duit.server.domain.common.dto.FieldError
import duit.server.infrastructure.external.discord.DiscordService
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanInstantiationException
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
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.LocalDateTime
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler(
    private val discordService: DiscordService
) {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(
        ex: EntityNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            errorCode = ErrorCode.NOT_FOUND,
            message = ex.message,
            request = request,
            ex = ex
        )
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            errorCode = ErrorCode.INVALID_REQUEST,
            message = ex.message,
            request = request,
            ex = ex
        )
    }
    
    @ExceptionHandler(BeanInstantiationException::class)
    fun handleBeanInstantiationException(
        ex: BeanInstantiationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val cause = ex.cause
        if (cause is IllegalArgumentException) {
            return buildErrorResponse(
                errorCode = ErrorCode.INVALID_REQUEST,
                message = cause.message,
                request = request,
                ex = ex
            )
        }
        return buildErrorResponse(
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR,
            message = null,
            request = request,
            ex = ex
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            errorCode = ErrorCode.CONFLICT,
            message = ex.message,
            request = request,
            ex = ex
        )
    }
    
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            errorCode = ErrorCode.UNAUTHORIZED,
            message = null,
            request = request,
            ex = ex
        )
    }
    
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            errorCode = ErrorCode.FORBIDDEN,
            message = null,
            request = request,
            ex = ex
        )
    }
    
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
            code = ErrorCode.VALIDATION_FAILED.name,
            message = ErrorCode.VALIDATION_FAILED.message,
            fieldErrors = fieldErrors,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
        )
        
        log.warn("Validation failed for request: {} - {}", request.requestURI, ex.message)
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
            code = ErrorCode.VALIDATION_FAILED.name,
            message = ErrorCode.VALIDATION_FAILED.message,
            fieldErrors = fieldErrors,
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
        )
        
        log.warn("Binding failed for request: {} - {}", request.requestURI, ex.message)
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            errorCode = ErrorCode.NOT_FOUND,
            message = "요청한 리소스를 찾을 수 없습니다",
            request = request,
            ex = ex
        )
    }

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
        val errorCode = when (ex) {
            is HttpRequestMethodNotSupportedException -> ErrorCode.METHOD_NOT_ALLOWED
            is NoHandlerFoundException -> ErrorCode.NOT_FOUND
            else -> ErrorCode.INVALID_REQUEST
        }
        
        return buildErrorResponse(
            errorCode = errorCode,
            message = null,
            request = request,
            ex = ex
        )
    }
    
    @ExceptionHandler(DataAccessException::class, DataIntegrityViolationException::class)
    fun handleDataAccessException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Database error occurred: ", ex)
        
        val errorCode = if (ex is DataIntegrityViolationException) {
            ErrorCode.DATA_INTEGRITY_VIOLATION
        } else {
            ErrorCode.DATA_ACCESS_ERROR
        }
        
        return buildErrorResponse(
            errorCode = errorCode,
            message = null,
            request = request,
            ex = ex
        )
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error occurred: ", ex)
        
        return buildErrorResponse(
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR,
            message = null,
            request = request,
            ex = ex
        )
    }
    
    private fun buildErrorResponse(
        errorCode: ErrorCode,
        message: String?,
        request: HttpServletRequest,
        ex: Exception
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            code = errorCode.name,
            message = message ?: errorCode.message,
            fieldErrors = emptyList(),
            timestamp = LocalDateTime.now(),
            path = request.requestURI,
        )
        
        when (errorCode.httpStatus.value()) {
            404 -> log.info("Not found: {}", request.requestURI)
            401, 403 -> log.info("Access denied: {} - {}", request.requestURI, errorCode.name)
            in 400..499 -> log.warn("Client error: {} - {}", request.requestURI, ex.message)
            in 500..599 -> {
                log.error("Server error: {} - {}", request.requestURI, ex.message, ex)

                // Discord 알림 전송
//                discordService.sendServerErrorNotification(
//                    errorCode = errorCode.name,
//                    message = errorResponse.message,
//                    path = request.requestURI,
//                    timestamp = errorResponse.timestamp,
//                    exception = ex
//                )
            }
        }
        
        return ResponseEntity.status(errorCode.httpStatus).body(errorResponse)
    }
}