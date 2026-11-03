package duit.server.application.config

import duit.server.support.IntegrationTestSupport
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 요청마다 드는 고정 비용 설정 (performance/decisions.md D-019).
 * A-1 이후 JFR 에서 Spring Security 필터별 관측 ~7%, 응답 들여쓰기 출력 2.6% 가 요청 스레드 CPU 로 잡혔다.
 */
@DisplayName("요청 고정 비용 설정")
class RequestOverheadIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Test
    fun `HTTP 요청 메트릭은 남기고 Spring Security 필터별 관측은 만들지 않는다`() {
        mockMvc.perform(get("/api/v2/events/count")).andExpect(status().isOk)

        assertNotNull(meterRegistry.find("http.server.requests").timer(), "대시보드가 쓰는 HTTP 요청 메트릭")
        val securityMeters = meterRegistry.meters.map { it.id.name }.filter { it.startsWith("spring.security") }
        assertEquals(emptyList<String>(), securityMeters)
    }

    @Test
    fun `응답 JSON 은 들여쓰기 없이 한 줄로 쓴다`() {
        val body = mockMvc.perform(get("/api/v2/events/count"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        assertFalse(body.contains('\n'), body)
    }
}
