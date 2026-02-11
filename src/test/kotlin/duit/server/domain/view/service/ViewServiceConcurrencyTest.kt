package duit.server.domain.view.service

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import duit.server.domain.view.entity.View
import duit.server.domain.view.repository.ViewRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ViewService 동시성 통합 테스트")
class ViewServiceConcurrencyTest {

    @Autowired
    private lateinit var viewService: ViewService

    @Autowired
    private lateinit var viewRepository: ViewRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Nested
    @DisplayName("createView 통합 테스트")
    inner class CreateViewIntegrationTests {

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
                val result = viewService.createView(event)

                // then
                assertNotNull(result.id, "저장 후 ID가 생성되어야 합니다")
                assertEquals(0, result.count, "초기 count는 0이어야 합니다")

                val found = transactionTemplate.execute {
                    viewRepository.findByEventId(event.id!!)
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
    @DisplayName("increaseCount 동시성 테스트")
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
                        viewService.increaseCount(testEventId!!)
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
                viewRepository.findByEventId(testEventId!!)
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
                        viewService.increaseCount(nonExistentEventId)
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
