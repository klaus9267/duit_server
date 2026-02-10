package duit.server.domain.view.controller

import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("View API 통합 테스트")
class ViewControllerIntegrationTest : IntegrationTestSupport() {

    private var eventId: Long = 0

    @BeforeEach
    fun setUp() {
        val host = TestFixtures.host()
        entityManager.persist(host)

        val event = TestFixtures.event(host = host, isApproved = true)
        entityManager.persist(event)

        val view = TestFixtures.view(event = event, count = 5)
        entityManager.persist(view)

        entityManager.flush()
        entityManager.clear()

        eventId = event.id!!
    }

    @Nested
    @DisplayName("PATCH /api/v1/views/{eventId} - 조회수 증가")
    inner class IncreaseCountTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("조회수가 정상적으로 증가한다")
            fun increaseViewCount() {
                mockMvc.perform(patch("/api/v1/views/{eventId}", eventId))
                    .andDo(print())
                    .andExpect(status().isNoContent)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("존재하지 않는 eventId로 요청하면 404를 반환한다")
            fun notFoundEvent() {
                mockMvc.perform(patch("/api/v1/views/{eventId}", 999999))
                    .andDo(print())
                    .andExpect(status().isNotFound)
            }
        }
    }
}
