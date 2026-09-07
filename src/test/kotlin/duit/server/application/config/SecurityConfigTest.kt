package duit.server.application.config

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class SecurityConfigTest {

    private val corsConfiguration = SecurityConfig(mockk(), mockk())
        .corsConfigurationSource()
        .getCorsConfiguration(MockHttpServletRequest("GET", "/api/v2/events"))!!

    @Test
    fun `운영 웹 도메인을 CORS origin으로 허용한다`() {
        listOf(
            "https://dutyit.net",
            "https://www.dutyit.net"
        ).forEach { origin ->
            assertEquals(origin, corsConfiguration.checkOrigin(origin))
        }
    }

    @Test
    fun `유사 도메인은 CORS origin으로 허용하지 않는다`() {
        assertNull(corsConfiguration.checkOrigin("https://malicious-dutyit.net"))
    }
}
