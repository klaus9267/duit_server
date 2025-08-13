package duit.server.application.common

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val message: String,
    val httpStatus: HttpStatus
) {
    // 공통 에러
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("잘못된 요청입니다", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("접근 권한이 없습니다", HttpStatus.FORBIDDEN),
    NOT_FOUND("요청한 리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("지원하지 않는 HTTP 메서드입니다", HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT("리소스 충돌이 발생했습니다", HttpStatus.CONFLICT),
    VALIDATION_FAILED("입력값 검증에 실패했습니다", HttpStatus.BAD_REQUEST),
    
    // Firebase 관련 에러
    INVALID_FIREBASE_TOKEN("유효하지 않은 Firebase 토큰입니다", HttpStatus.BAD_REQUEST),
    FIREBASE_VERIFICATION_FAILED("Firebase 토큰 검증에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    FIREBASE_USER_NOT_FOUND("Firebase 사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    
    // 데이터베이스 관련 에러
    DATA_ACCESS_ERROR("데이터베이스 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_INTEGRITY_VIOLATION("데이터 제약 조건 위반입니다", HttpStatus.CONFLICT)
}