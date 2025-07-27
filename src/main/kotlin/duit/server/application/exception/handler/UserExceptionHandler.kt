package duit.server.application.exception.handler

import duit.server.application.common.ErrorCode
import duit.server.application.controller.dto.ErrorResponse
import duit.server.application.exception.ErrorResponseBuilder
import duit.server.domain.user.exception.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 사용자 도메인 전용 예외 처리 핸들러 (간소화)
 */
@RestControllerAdvice
@Order(1) // 우선순위 1 - 가장 먼저 처리
class UserExceptionHandler(
    private val errorResponseBuilder: ErrorResponseBuilder
) {
    
    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(
        ex: UserNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.USER_NOT_FOUND,
            exception = ex,
            request = request,
            additionalDetails = "사용자 ID ${ex.userId}를 찾을 수 없습니다."
        )
    }
    
    @ExceptionHandler(UserLoginIdNotFoundException::class)
    fun handleUserLoginIdNotFoundException(
        ex: UserLoginIdNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.USER_NOT_FOUND,
            exception = ex,
            request = request,
            additionalDetails = "로그인 ID '${ex.loginId}'로 등록된 사용자가 없습니다."
        )
    }
    
    @ExceptionHandler(UserEmailNotFoundException::class)
    fun handleUserEmailNotFoundException(
        ex: UserEmailNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.USER_EMAIL_NOT_FOUND,
            exception = ex,
            request = request,
            additionalDetails = "이메일 ${ex.email}로 등록된 사용자가 없습니다."
        )
    }
    
    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmailException(
        ex: DuplicateEmailException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.DUPLICATE_EMAIL,
            exception = ex,
            request = request,
            additionalDetails = "이메일 ${ex.email}는 이미 사용 중입니다."
        )
    }
    
    @ExceptionHandler(DuplicateLoginIdException::class)
    fun handleDuplicateLoginIdException(
        ex: DuplicateLoginIdException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.DUPLICATE_LOGIN_ID,
            exception = ex,
            request = request,
            additionalDetails = "로그인 ID '${ex.loginId}'는 이미 사용 중입니다."
        )
    }
    
    @ExceptionHandler(DuplicateNicknameException::class)
    fun handleDuplicateNicknameException(
        ex: DuplicateNicknameException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.DUPLICATE_NICKNAME,
            exception = ex,
            request = request,
            additionalDetails = "닉네임 '${ex.nickname}'은 이미 사용 중입니다."
        )
    }
    
    @ExceptionHandler(InvalidPasswordException::class)
    fun handleInvalidPasswordException(
        ex: InvalidPasswordException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.INVALID_PASSWORD,
            exception = ex,
            request = request,
            additionalDetails = "비밀번호가 일치하지 않습니다."
        )
    }
}
