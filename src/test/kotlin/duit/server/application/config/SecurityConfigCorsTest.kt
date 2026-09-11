package duit.server.application.config

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.cors.DefaultCorsProcessor

class SecurityConfigCorsTest {
    private val source = SecurityConfig(mockk(), mockk()).corsConfigurationSource()

    @ParameterizedTest
    @CsvSource(
        "https://dutyit.web.tossmini.com, GET, /api/v2/events",
        "https://dutyit.private-web.tossmini.com, GET, /api/v1/job-postings",
        "https://dutyit.web.tossmini.com, PATCH, /api/v1/views/729",
        "https://dutyit.private-web.tossmini.com, PATCH, /api/v1/views/729"
    )
    fun `앱인토스의 기존 API preflight를 허용한다`(origin: String, method: String, path: String) {
        val request = MockHttpServletRequest("OPTIONS", path).apply {
            addHeader("Origin", origin)
            addHeader("Access-Control-Request-Method", method)
        }
        val response = MockHttpServletResponse()

        assertTrue(DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, response))
        assertEquals(200, response.status)
        assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"))
        assertTrue(response.getHeader("Access-Control-Allow-Methods")!!.contains(method))
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://other.web.tossmini.com", "https://dutyit.web.tossmini.com.evil.test"])
    fun `다른 미니앱과 위장 출처는 허용하지 않는다`(origin: String) {
        val request = MockHttpServletRequest("OPTIONS", "/api/v2/events").apply {
            addHeader("Origin", origin)
            addHeader("Access-Control-Request-Method", "GET")
        }
        val response = MockHttpServletResponse()

        DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, response)

        assertEquals(403, response.status)
    }
}
