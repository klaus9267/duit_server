package duit.server.application.config

import com.zaxxer.hikari.HikariDataSource
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.User
import duit.server.domain.view.repository.ViewRepository
import duit.server.support.IntegrationTestSupport
import duit.server.support.JdbcCallCounter
import duit.server.support.JdbcCallCounterConfig
import duit.server.support.fixture.TestFixtures
import io.micrometer.core.instrument.MeterRegistry
import jakarta.persistence.EntityManagerFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.ClassUtils
import java.lang.reflect.AnnotatedElement
import java.sql.Connection
import javax.sql.DataSource

/**
 * A-1: 캐시가 맞는 요청은 DB 커넥션을 잡지 않고, 트랜잭션 관리 문장(SET … / COMMIT)도 내지 않는다.
 * A-2: 로그인 요청도 북마크 ID 캐시가 맞으면 같다 (decisions.md D-020).
 *
 * 테스트 트랜잭션에 합류하면 요청 단위의 커넥션 획득·커밋을 관찰할 수 없으므로 NOT_SUPPORTED 로 실행하고,
 * 픽스처는 TransactionTemplate 으로 커밋한 뒤 직접 지운다. (performance/decisions.md D-016)
 */
@Import(JdbcCallCounterConfig::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("커넥션 처리 정책 (A-1)")
class ConnectionHandlingIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired
    private lateinit var viewRepository: ViewRepository

    private val hikari by lazy { dataSource.unwrap(HikariDataSource::class.java) }

    private lateinit var host: Host
    private lateinit var event: Event
    private lateinit var user: User

    @BeforeEach
    fun setUpCommittedFixtures() {
        transactionTemplate.executeWithoutResult {
            host = TestFixtures.host(name = "커넥션 테스트 주최").also(entityManager::persist)
            event = TestFixtures.event(
                title = "커넥션 테스트 행사",
                status = EventStatus.RECRUITING,
                statusGroup = EventStatusGroup.ACTIVE,
                host = host,
            ).also(entityManager::persist)
            user = TestFixtures.user(providerId = "connection-handling-user", nickname = "커넥션테스트").also(entityManager::persist)
            entityManager.persist(TestFixtures.bookmark(user = user, event = event))
        }
        awaitPoolFilled()
    }

    @AfterEach
    fun deleteCommittedFixtures() {
        transactionTemplate.executeWithoutResult {
            entityManager.createQuery("DELETE FROM Bookmark b WHERE b.user.id = :id").setParameter("id", user.id).executeUpdate()
            entityManager.createQuery("DELETE FROM View v WHERE v.event.id = :id").setParameter("id", event.id).executeUpdate()
            entityManager.createQuery("DELETE FROM Event e WHERE e.id = :id").setParameter("id", event.id).executeUpdate()
            entityManager.createQuery("DELETE FROM Host h WHERE h.id = :id").setParameter("id", host.id).executeUpdate()
            entityManager.createQuery("DELETE FROM User u WHERE u.id = :id").setParameter("id", user.id).executeUpdate()
        }
    }

    @Nested
    @DisplayName("캐시 히트 경로")
    inner class CacheHit {

        @Test
        @DisplayName("비로그인 캐시 히트는 커넥션을 잡지 않고 JDBC 호출도 0이다")
        fun anonymousCacheHitUsesNoConnection() {
            getActiveEvents().andExpect(status().isOk) // 캐시 미스 → 적재

            val usage = measure { getActiveEvents().andExpect(status().isOk) }

            assertEquals(DbUsage(acquisitions = 0, jdbcCalls = emptyMap()), usage)
        }

        @Test
        @DisplayName("로그인 캐시 히트도 북마크 ID 가 캐시돼 있으면 커넥션을 잡지 않는다 (A-2)")
        fun authenticatedCacheHitUsesNoConnection() {
            getActiveEvents(authHeader(user.id!!)).andExpect(status().isOk) // 목록·북마크 ID 캐시 적재

            val usage = measure {
                getActiveEvents(authHeader(user.id!!))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content[?(@.id == ${event.id})].isBookmarked").value(true))
            }

            assertEquals(DbUsage(acquisitions = 0, jdbcCalls = emptyMap()), usage)
        }

        @Test
        @DisplayName("커넥션 풀이 고갈돼도 비로그인 캐시 히트는 즉시 응답한다 (격벽)")
        fun cacheHitSurvivesPoolExhaustion() {
            getActiveEvents().andExpect(status().isOk)
            val held = mutableListOf<Connection>()

            try {
                repeat(hikari.maximumPoolSize) { held += dataSource.connection }
                val startedAt = System.nanoTime()
                getActiveEvents().andExpect(status().isOk)
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

                assertTrue(elapsedMs < hikari.connectionTimeout / 3, "풀 고갈 중 캐시 히트가 ${elapsedMs}ms 걸림")
            } finally {
                held.forEach { it.close() }
            }
        }
    }

    @Nested
    @DisplayName("DB 를 쓰는 경로의 정합성")
    inner class Consistency {

        @Test
        @DisplayName("쓰기 요청은 커밋된다 (조회수 증가)")
        fun writeIsCommitted() {
            val usage = measure { mockMvc.perform(patch("/api/v1/views/${event.id}")).andExpect(status().isNoContent) }

            assertEquals(1, transactionTemplate.execute { viewRepository.findByEventId(event.id!!)!!.count })
            assertEquals(1, usage.jdbcCalls["commit"])
        }

        @Test
        @DisplayName("롤백된 쓰기 트랜잭션은 반영되지 않는다 (풀 autoCommit=false 에서도 자동 커밋 없음)")
        fun rolledBackWriteIsDiscarded() {
            val name = "롤백될 주최-${System.nanoTime()}"

            transactionTemplate.executeWithoutResult { tx ->
                entityManager.persist(TestFixtures.host(name = name))
                entityManager.flush()
                tx.setRollbackOnly()
            }

            val count = transactionTemplate.execute {
                entityManager.createQuery("SELECT COUNT(h) FROM Host h WHERE h.name = :name", Long::class.javaObjectType)
                    .setParameter("name", name).singleResult.toLong()
            }
            assertEquals(0L, count)
        }

        @Test
        @DisplayName("행사 상세는 트랜잭션 밖 지연 로딩 없이 주최 정보까지 응답한다 (OSIV off)")
        fun eventDetailLoadsHostWithinTransaction() {
            mockMvc.perform(get("/api/v2/events/${event.id}").header("Authorization", authHeader(user.id!!)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.host.name").value(host.name))
                .andExpect(jsonPath("$.isBookmarked").value(true))
        }
    }

    @Nested
    @DisplayName("설정 가드")
    inner class ConfigurationGuard {

        @Test
        @DisplayName("풀 autoCommit=false 이고 Hibernate 가 커넥션을 첫 SQL 에 잡아 트랜잭션 끝에 반납한다")
        fun connectionHandlingMatchesProduction() {
            val options = entityManagerFactory.unwrap(SessionFactoryImplementor::class.java).sessionFactoryOptions

            assertAll(
                { assertFalse(hikari.isAutoCommit, "spring.datasource.hikari.auto-commit") },
                { assertTrue(options.doesConnectionProviderDisableAutoCommit(), "Boot 가 provider_disables_autocommit 을 유도해야 한다") },
                {
                    assertEquals(
                        PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION,
                        options.physicalConnectionHandlingMode,
                    )
                },
            )
        }

        @Test
        @DisplayName("@Transactional 에 isolation 을 지정하지 않는다 (이 모드에선 InvalidIsolationLevelException)")
        fun noCustomIsolation() {
            // 활성 빈이 아니라 클래스패스 전체를 본다: @Profile("prod") 스케줄러처럼 테스트 컨텍스트에 없는 클래스도 검사해야 한다.
            // 기본 스캐너는 @Profile 같은 조건과 인터페이스·추상 여부로 걸러내므로 두 판정을 모두 연다.
            val allTypes = object : ClassPathScanningCandidateComponentProvider(false) {
                override fun isCandidateComponent(metadataReader: MetadataReader) = true
                override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition) = true
            }

            val violations = allTypes.findCandidateComponents("duit.server")
                .map { ClassUtils.forName(it.beanClassName!!, javaClass.classLoader) }
                .flatMap { type -> listOf<AnnotatedElement>(type) + type.declaredMethods }
                .filter {
                    val tx = AnnotatedElementUtils.findMergedAnnotation(it, Transactional::class.java)
                    tx != null && tx.isolation != Isolation.DEFAULT
                }

            assertEquals(emptyList<AnnotatedElement>(), violations)
        }
    }

    private fun getActiveEvents(authorization: String? = null): ResultActions =
        mockMvc.perform(
            get("/api/v2/events")
                .param("field", "CREATED_AT")
                .param("size", "4")
                .param("statusGroup", "ACTIVE")
                .apply { authorization?.let { header("Authorization", it) } }
        )

    private data class DbUsage(val acquisitions: Long, val jdbcCalls: Map<String, Int>)

    /** 요청 1건 동안의 물리 커넥션 획득 수(Hikari)와 드라이버 호출 수. 요청 후 반납 누락이 없는지도 확인한다. */
    private fun measure(request: () -> Unit): DbUsage {
        val acquiredBefore = acquisitions()
        JdbcCallCounter.reset()

        request()

        assertEquals(0, hikari.hikariPoolMXBean.activeConnections, "요청이 끝났는데 반납되지 않은 커넥션")
        return DbUsage(acquisitions() - acquiredBefore, JdbcCallCounter.snapshot())
    }

    private fun acquisitions(): Long = meterRegistry.get("hikaricp.connections.acquire").timer().count()

    /** 풀이 백그라운드로 커넥션을 채우는 중이면 그 setAutoCommit 호출이 측정에 섞이므로 다 찰 때까지 기다린다. */
    private fun awaitPoolFilled() {
        val deadline = System.nanoTime() + POOL_FILL_TIMEOUT_NANOS
        while (hikari.hikariPoolMXBean.totalConnections < hikari.minimumIdle && System.nanoTime() < deadline) {
            Thread.sleep(20)
        }
    }

    companion object {
        private const val POOL_FILL_TIMEOUT_NANOS = 5_000_000_000L
    }
}
