package duit.server.domain.view.service

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import duit.server.domain.view.entity.View
import duit.server.domain.view.repository.ViewRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

@DisplayName("ViewService 단위 테스트")
class ViewServiceUnitTest {

    @Nested
    @DisplayName("createView")
    inner class CreateViewTests {

        private lateinit var mockViewRepository: ViewRepository
        private lateinit var viewService: ViewService
        private lateinit var host: Host
        private lateinit var event: Event

        @BeforeEach
        fun setUp() {
            mockViewRepository = mockk()
            viewService = ViewService(mockViewRepository)
            host = Host(id = 1L, name = "테스트 주최")
            event = Event(
                id = 1L,
                title = "테스트 행사",
                startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null,
                recruitmentEndAt = null,
                uri = "https://example.com/event",
                thumbnail = null,
                eventType = EventType.CONFERENCE,
                host = host
            )
        }

        @Test
        @DisplayName("정상 생성 - Event로 View를 생성하고 반환한다")
        fun createViewSuccess() {
            // given
            val savedView = View(id = 1L, event = event, count = 0)
            every { mockViewRepository.save(any<View>()) } returns savedView

            // when
            val result = viewService.createView(event)

            // then
            assertEquals(savedView.id, result.id)
            assertEquals(0, result.count)
            verify(exactly = 1) { mockViewRepository.save(any<View>()) }
        }

        @Test
        @DisplayName("생성 시 count=0, event 매핑이 올바른지 검증")
        fun createViewPassesCorrectArguments() {
            // given
            val viewSlot = slot<View>()
            every { mockViewRepository.save(capture(viewSlot)) } answers { viewSlot.captured }

            // when
            viewService.createView(event)

            // then
            val captured = viewSlot.captured
            assertEquals(0, captured.count, "초기 count는 0이어야 합니다")
            assertEquals(event, captured.event, "전달된 event가 일치해야 합니다")
        }

        @Test
        @DisplayName("저장 실패 시 DataIntegrityViolationException 전파")
        fun createViewThrowsOnDuplicateEvent() {
            // given
            every { mockViewRepository.save(any<View>()) } throws DataIntegrityViolationException("unique constraint violation")

            // when & then
            assertThrows<DataIntegrityViolationException> {
                viewService.createView(event)
            }
        }
    }

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
