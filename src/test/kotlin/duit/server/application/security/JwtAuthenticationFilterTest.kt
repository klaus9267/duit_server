package duit.server.application.security

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val filter = JwtAuthenticationFilter(jwtTokenProvider)

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    @Test
    fun `토큰은 요청마다 한 번만 파싱한다`() {
        every { jwtTokenProvider.getAuthenticationOrNull("token") } returns
            UsernamePasswordAuthenticationToken(7L, null, emptyList())

        filter.doFilter(requestWith("Bearer token"), MockHttpServletResponse(), MockFilterChain())

        verify(exactly = 1) { jwtTokenProvider.getAuthenticationOrNull("token") }
        confirmVerified(jwtTokenProvider)
        assertEquals(7L, SecurityContextHolder.getContext().authentication?.principal)
    }

    @Test
    fun `유효하지 않은 토큰이면 인증 정보를 비워 둔다`() {
        every { jwtTokenProvider.getAuthenticationOrNull("bad") } returns null

        filter.doFilter(requestWith("Bearer bad"), MockHttpServletResponse(), MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Bearer 헤더가 없으면 토큰을 보지 않는다`() {
        filter.doFilter(MockHttpServletRequest("GET", "/api/v2/events"), MockHttpServletResponse(), MockFilterChain())

        confirmVerified(jwtTokenProvider)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    private fun requestWith(authorization: String) =
        MockHttpServletRequest("GET", "/api/v2/events").apply { addHeader("Authorization", authorization) }
}
