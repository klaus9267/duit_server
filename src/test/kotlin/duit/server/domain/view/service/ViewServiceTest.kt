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
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ViewService Test")
class ViewServiceTest {

    @Autowired
    private lateinit var autowiredViewService: ViewService

    @Autowired
    private lateinit var autowiredViewRepository: ViewRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

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

        @Test
        @DisplayName("통합 - DB에 View가 실제로 저장되고 count=0으로 초기화된다")
        fun createViewIntegration() {
            // given
            val host = transactionTemplate.execute {
                val h = Host(name = "통합 테스트 주최")
                entityManager.persist(h)
                entityManager.flush()
                h
            }!!
            val event = transactionTemplate.execute {
                val e = Event(
                    title = "통합 테스트 행사",
                    startAt = LocalDateTime.now().plusDays(7),
                    endAt = LocalDateTime.now().plusDays(8),
                    recruitmentStartAt = null,
                    recruitmentEndAt = null,
                    uri = "https://example.com/event",
                    thumbnail = null,
                    eventType = EventType.CONFERENCE,
                    host = host
                )
                entityManager.persist(e)
                entityManager.flush()
                e
            }!!

            try {
                // when
                val result = autowiredViewService.createView(event)

                // then
                assertNotNull(result.id, "저장 후 ID가 생성되어야 합니다")
                assertEquals(0, result.count, "초기 count는 0이어야 합니다")

                val found = transactionTemplate.execute {
                    autowiredViewRepository.findByEventId(event.id!!)
                }
                assertNotNull(found, "DB에서 조회 가능해야 합니다")
                assertEquals(0, found!!.count)
            } finally {
                // cleanup — JPQL bulk deletes in FK order (View → Event → Host)
                transactionTemplate.execute {
                    entityManager.createQuery("DELETE FROM View v WHERE v.event.id = :id")
                        .setParameter("id", event.id).executeUpdate()
                    entityManager.createQuery("DELETE FROM Event e WHERE e.id = :id")
                        .setParameter("id", event.id).executeUpdate()
                    entityManager.createQuery("DELETE FROM Host h WHERE h.id = :id")
                        .setParameter("id", host.id).executeUpdate()
                }
            }
        }
    }

    @Nested
    @DisplayName("increaseCount")
    inner class IncreaseCountTests {

        @Nested
        @DisplayName("단위 테스트")
        inner class UnitTests {

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

        @Nested
        @DisplayName("동시성 통합 테스트")
        inner class ConcurrencyTests {

            private var testEventId: Long? = null
            private var testHostId: Long? = null

            @AfterEach
            fun tearDown() {
                transactionTemplate.execute {
                    testEventId?.let { eventId ->
                        entityManager.createQuery("DELETE FROM View v WHERE v.event.id = :id")
                            .setParameter("id", eventId).executeUpdate()
                        entityManager.createQuery("DELETE FROM Event e WHERE e.id = :id")
                            .setParameter("id", eventId).executeUpdate()
                    }
                    testHostId?.let { hostId ->
                        entityManager.createQuery("DELETE FROM Host h WHERE h.id = :id")
                            .setParameter("id", hostId).executeUpdate()
                    }
                }
            }

            @BeforeEach
            fun setUp() {
                val host = transactionTemplate.execute {
                    val h = Host(name = "동시성 테스트 주최")
                    entityManager.persist(h)
                    entityManager.flush()
                    h
                }!!
                testHostId = host.id

                val event = transactionTemplate.execute {
                    val e = Event(
                        title = "동시성 테스트 행사",
                        startAt = LocalDateTime.now().plusDays(7),
                        endAt = LocalDateTime.now().plusDays(8),
                        recruitmentStartAt = null,
                        recruitmentEndAt = null,
                        uri = "https://example.com/event",
                        thumbnail = null,
                        eventType = EventType.CONFERENCE,
                        host = host
                    )
                    entityManager.persist(e)
                    entityManager.flush()
                    e
                }!!
                testEventId = event.id

                transactionTemplate.execute {
                    val v = View(event = event, count = 0)
                    entityManager.persist(v)
                    entityManager.flush()
                }
            }

            @Test
            @DisplayName("50개 스레드 동시 호출 시 최종 count == 50")
            fun concurrentIncrementCountConsistency() {
                // given
                val threadCount = 50
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val failureCount = AtomicInteger(0)

                // when
                repeat(threadCount) {
                    executor.submit {
                        try {
                            autowiredViewService.increaseCount(testEventId!!)
                        } catch (e: Exception) {
                            failureCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                latch.await()
                executor.shutdown()

                // then
                assertEquals(0, failureCount.get(), "동시 조회수 증가 중 실패가 발생했습니다")

                val updatedView = transactionTemplate.execute {
                    autowiredViewRepository.findByEventId(testEventId!!)
                }
                assertNotNull(updatedView, "View가 존재해야 합니다")
                assertEquals(threadCount, updatedView!!.count, "최종 조회수는 스레드 수와 같아야 합니다")
            }

            @Test
            @DisplayName("존재하지 않는 eventId로 동시 호출 시 모든 스레드에서 EntityNotFoundException 발생")
            fun concurrentIncrementNonExistentEvent() {
                // given
                val nonExistentEventId = 99999L
                val threadCount = 10
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val exceptionCount = AtomicInteger(0)
                val unexpectedExceptionCount = AtomicInteger(0)

                // when
                repeat(threadCount) {
                    executor.submit {
                        try {
                            autowiredViewService.increaseCount(nonExistentEventId)
                        } catch (e: EntityNotFoundException) {
                            exceptionCount.incrementAndGet()
                        } catch (e: Exception) {
                            unexpectedExceptionCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                latch.await()
                executor.shutdown()

                // then
                assertEquals(threadCount, exceptionCount.get(), "모든 스레드에서 EntityNotFoundException이 발생해야 합니다")
                assertEquals(0, unexpectedExceptionCount.get(), "예상치 못한 예외가 발생했습니다")
            }
        }
    }
}
