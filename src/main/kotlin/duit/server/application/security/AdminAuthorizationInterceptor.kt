package duit.server.application.security

import duit.server.application.common.RequireAdmin
import duit.server.domain.admin.repository.AdminRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/**
 * @RequireAdmin 어노테이션이 붙은 핸들러 메서드 호출 시
 * 인증된 사용자가 admins 테이블에 등록된 관리자인지 검증한다.
 *
 * - 인증 자체가 안 된 경우: AuthenticationCredentialsNotFoundException → 401
 * - 인증은 됐으나 관리자 아님: AccessDeniedException → 403
 */
@Component
class AdminAuthorizationInterceptor(
    private val adminRepository: AdminRepository,
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        if (handler.getMethodAnnotation(RequireAdmin::class.java) == null) return true

        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw AuthenticationCredentialsNotFoundException("인증이 필요합니다.")

        val userId = authentication.principal as? Long
            ?: throw AuthenticationCredentialsNotFoundException("인증이 필요합니다.")

        if (!adminRepository.existsByUserId(userId)) {
            throw AccessDeniedException("관리자 권한이 필요합니다.")
        }
        return true
    }
}
