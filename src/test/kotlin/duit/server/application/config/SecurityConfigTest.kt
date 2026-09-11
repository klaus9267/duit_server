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
    fun `듀잇 웹과 앱인토스 도메인을 CORS origin으로 허용한다`() {
        listOf(
            "https://dutyit.net",
            "https://www.dutyit.net",
            "https://dutyit.web.tossmini.com",
            "https://dutyit.private-web.tossmini.com"
        ).forEach { origin ->
            assertEquals(origin, corsConfiguration.checkOrigin(origin))
        }
    }

    @Test
    fun `유사 도메인은 CORS origin으로 허용하지 않는다`() {
        assertNull(corsConfiguration.checkOrigin("https://malicious-dutyit.net"))
    }
}
