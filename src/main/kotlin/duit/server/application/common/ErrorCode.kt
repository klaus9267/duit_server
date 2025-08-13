package duit.server.application.common

import org.springframework.http.HttpStatus

/**
 * 에러 코드 정의 - 타입 안전한 방식으로 개선됨
 */
enum class ErrorCode(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus
) {
    // 공통 에러
    INTERNAL_SERVER_ERROR("COMMON_001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("COMMON_002", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("COMMON_003", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON_004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("COMMON_005", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("COMMON_006", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT("COMMON_007", "리소스 충돌이 발생했습니다.", HttpStatus.CONFLICT),
    VALIDATION_FAILED("COMMON_008", "입력값 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),

    // 사용자 관련 에러
    USER_NOT_FOUND("USER_001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_EMAIL_NOT_FOUND("USER_002", "해당 이메일로 등록된 사용자가 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("USER_003", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_LOGIN_ID("USER_004", "이미 사용 중인 로그인 ID입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME("USER_005", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    INVALID_PASSWORD("USER_006", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    
    // 이벤트 관련 에러
    EVENT_NOT_FOUND("EVENT_001", "이벤트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    EVENT_CAPACITY_EXCEEDED("EVENT_002", "이벤트 정원을 초과했습니다.", HttpStatus.CONFLICT),
    EVENT_REGISTRATION_CLOSED("EVENT_003", "이벤트 등록이 마감되었습니다.", HttpStatus.CONFLICT),
    
    // Firebase 관련 에러
    INVALID_FIREBASE_TOKEN("FIREBASE_001", "유효하지 않은 Firebase 토큰입니다.", HttpStatus.BAD_REQUEST),
    FIREBASE_VERIFICATION_FAILED("FIREBASE_002", "Firebase 토큰 검증에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FIREBASE_USER_NOT_FOUND("FIREBASE_003", "Firebase 사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    
    // 애플리케이션 서비스 에러
    EXTERNAL_SERVICE_ERROR("APP_001", "외부 서비스 연동 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    DATA_ACCESS_ERROR("APP_002", "데이터 접근 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
}
