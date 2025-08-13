package duit.server.application.exception.handler

import duit.server.application.common.ErrorCode
import duit.server.application.exception.ErrorResponseBuilder
import duit.server.domain.common.dto.ErrorResponse
import duit.server.infrastructure.external.firebase.exception.InvalidFirebaseTokenException
import duit.server.infrastructure.external.firebase.exception.FirebaseVerificationFailedException
import duit.server.infrastructure.external.firebase.exception.FirebaseUserNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(2)
class FirebaseExceptionHandler(
    private val errorResponseBuilder: ErrorResponseBuilder
) {

    @ExceptionHandler(InvalidFirebaseTokenException::class)
    fun handleInvalidFirebaseTokenException(
        ex: InvalidFirebaseTokenException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.INVALID_FIREBASE_TOKEN,
            exception = ex,
            request = request,
            additionalDetails = "유효하지 않은 Firebase 토큰입니다."
        )
    }

    @ExceptionHandler(FirebaseVerificationFailedException::class)
    fun handleFirebaseVerificationFailedException(
        ex: FirebaseVerificationFailedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.FIREBASE_VERIFICATION_FAILED,
            exception = ex,
            request = request,
            additionalDetails = "Firebase 토큰 검증에 실패했습니다."
        )
    }

    @ExceptionHandler(FirebaseUserNotFoundException::class)
    fun handleFirebaseUserNotFoundException(
        ex: FirebaseUserNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return errorResponseBuilder.buildDomainErrorResponse(
            errorCode = ErrorCode.FIREBASE_USER_NOT_FOUND,
            exception = ex,
            request = request,
            additionalDetails = "Firebase 사용자를 찾을 수 없습니다. UID: ${ex.uid}"
        )
    }
}