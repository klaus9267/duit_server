package duit.server.infrastructure.external.firebase.exception

import duit.server.domain.common.exception.DomainException

/**
 * Firebase 관련 순수 도메인 예외들 (HTTP 의존성 없음)
 */
class InvalidFirebaseTokenException(val token: String? = null, detailMessage: String? = null) : 
    DomainException("유효하지 않은 Firebase 토큰입니다.${detailMessage?.let { " - $it" } ?: ""}")

class FirebaseVerificationFailedException(detailMessage: String? = null) : 
    DomainException("Firebase 토큰 검증에 실패했습니다.${detailMessage?.let { " - $it" } ?: ""}")

class FirebaseUserNotFoundException(val uid: String) : 
    DomainException("Firebase 사용자를 찾을 수 없습니다. UID: $uid")