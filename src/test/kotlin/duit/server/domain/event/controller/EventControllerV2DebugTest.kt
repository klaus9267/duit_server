package duit.server.domain.event.controller

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Event API v2 응답 구조 디버그 테스트")
class EventControllerV2DebugTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("기본 API 호출 응답 구조 확인")
    fun `debug basic API response structure`() {
        val result = mockMvc.perform(
            get("/api/v2/events")
                .param("size", "3")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn()

        val responseBody = result.response.contentAsString
        println("=== 실제 응답 구조 ===")
        println(responseBody)
        
        // 응답이 JSON인지 확인
        if (responseBody.startsWith("{")) {
            println("=== JSON 응답 확인됨 ===")
        } else {
            println("=== 비정상 응답 (JSON 아님) ===")
        }
    }

    @Test
    @DisplayName("EventType 파라미터 테스트")
    fun `debug EventType parameter`() {
        mockMvc.perform(
            get("/api/v2/events")
                .param("types", "SEMINAR")
                .param("size", "3")
        )
            .andDo(print())
            .andExpect(status().isOk)
    }
}