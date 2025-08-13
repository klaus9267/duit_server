package duit.server.application.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityUtil {

    /**
     * 현재 인증된 사용자의 ID 조회
     *
     * @return 현재 사용자 ID
     * @throws IllegalStateException 인증되지 않은 사용자인 경우
     */
    fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("인증되지 않은 사용자입니다")

        return when (val principal = authentication.principal) {
            is Long -> principal
            is String -> principal.toLongOrNull()
                ?: throw IllegalStateException("유효하지 않은 사용자 정보입니다")

            else -> throw IllegalStateException("인증되지 않은 사용자입니다")
        }
    }
}
