package duit.server.application.security

import duit.server.application.common.RequireAdmin
import duit.server.domain.admin.repository.AdminRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.method.HandlerMethod
import java.lang.reflect.Method

@DisplayName("AdminAuthorizationInterceptor 단위 테스트")
class AdminAuthorizationInterceptorTest {

    private lateinit var adminRepository: AdminRepository
    private lateinit var interceptor: AdminAuthorizationInterceptor
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse

    private val adminAnnotatedMethod: Method =
        AnnotatedHandlers::class.java.getMethod("adminOnly")
    private val plainMethod: Method =
        AnnotatedHandlers::class.java.getMethod("plain")

    @BeforeEach
    fun setUp() {
        adminRepository = mockk()
        interceptor = AdminAuthorizationInterceptor(adminRepository)
        request = mockk(relaxed = true)
        response = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun handlerMethod(method: Method): HandlerMethod =
        HandlerMethod(AnnotatedHandlers(), method)

    private fun authenticate(userId: Any?) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                userId,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
    }

    @Nested
    @DisplayName("@RequireAdmin 이 붙은 핸들러")
    inner class WithAnnotation {

        @Test
        @DisplayName("관리자면 통과한다 (true 반환)")
        fun passesWhenAdmin() {
            authenticate(42L)
            every { adminRepository.existsByUserId(42L) } returns true

            val result = interceptor.preHandle(request, response, handlerMethod(adminAnnotatedMethod))

            assertTrue(result)
            verify(exactly = 1) { adminRepository.existsByUserId(42L) }
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException 을 던진다")
        fun deniesWhenNotAdmin() {
            authenticate(42L)
            every { adminRepository.existsByUserId(42L) } returns false

            val ex = assertThrows<AccessDeniedException> {
                interceptor.preHandle(request, response, handlerMethod(adminAnnotatedMethod))
            }
            assertEquals("관리자 권한이 필요합니다.", ex.message)
        }

        @Test
        @DisplayName("인증 정보가 없으면 AuthenticationCredentialsNotFoundException 을 던진다")
        fun throwsWhenNoAuthentication() {
            // SecurityContext 를 비워둠
            assertThrows<AuthenticationCredentialsNotFoundException> {
                interceptor.preHandle(request, response, handlerMethod(adminAnnotatedMethod))
            }
            verify(exactly = 0) { adminRepository.existsByUserId(any()) }
        }

        @Test
        @DisplayName("principal 이 Long 타입이 아니면 AuthenticationCredentialsNotFoundException 을 던진다")
        fun throwsWhenPrincipalNotLong() {
            authenticate("string-principal")

            assertThrows<AuthenticationCredentialsNotFoundException> {
                interceptor.preHandle(request, response, handlerMethod(adminAnnotatedMethod))
            }
            verify(exactly = 0) { adminRepository.existsByUserId(any()) }
        }
    }

    @Nested
    @DisplayName("@RequireAdmin 이 없는 핸들러")
    inner class WithoutAnnotation {

        @Test
        @DisplayName("관리자 검증 없이 통과한다")
        fun passesWithoutCheck() {
            // 인증 안 된 상태로도 통과해야 함
            val result = interceptor.preHandle(request, response, handlerMethod(plainMethod))

            assertTrue(result)
            verify(exactly = 0) { adminRepository.existsByUserId(any()) }
        }
    }

    @Nested
    @DisplayName("HandlerMethod 가 아닌 핸들러")
    inner class NonHandlerMethod {

        @Test
        @DisplayName("ResourceHttpRequestHandler 같은 비-MVC 핸들러는 무조건 통과한다")
        fun passesForNonHandlerMethod() {
            val result = interceptor.preHandle(request, response, Any())

            assertTrue(result)
            verify(exactly = 0) { adminRepository.existsByUserId(any()) }
        }
    }

    /**
     * 어노테이션 처리 검증용 더미 핸들러 클래스.
     * 실 컨트롤러를 대신함.
     */
    class AnnotatedHandlers {
        @RequireAdmin
        fun adminOnly() = Unit

        fun plain() = Unit
    }
}
