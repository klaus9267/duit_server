package duit.server.domain.view.service

import duit.server.domain.view.repository.ViewRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("ViewService 단위 테스트")
class ViewServiceUnitTest {

    @Nested
    @DisplayName("increaseCount")
    inner class IncreaseCountTests {

        private lateinit var mockViewRepository: ViewRepository
        private lateinit var viewService: ViewService

        @BeforeEach
        fun setUp() {
            mockViewRepository = mockk()
            viewService = ViewService(mockViewRepository)
        }

        @Test
        @DisplayName("정상 증가 - incrementCount 반환 1이면 정상 처리")
        fun increaseCountSuccess() {
            // given
            val eventId = 1L
            every { mockViewRepository.incrementCount(eventId) } returns 1

            // when & then
            assertDoesNotThrow { viewService.increaseCount(eventId) }
            verify(exactly = 1) { mockViewRepository.incrementCount(eventId) }
        }

        @Test
        @DisplayName("존재하지 않는 eventId - incrementCount 반환 0이면 EntityNotFoundException")
        fun increaseCountThrowsWhenNotFound() {
            // given
            val eventId = 999L
            every { mockViewRepository.incrementCount(eventId) } returns 0

            // when & then
            val exception = assertThrows<EntityNotFoundException> {
                viewService.increaseCount(eventId)
            }
            assertTrue(exception.message!!.contains("999"))
        }
    }
}
