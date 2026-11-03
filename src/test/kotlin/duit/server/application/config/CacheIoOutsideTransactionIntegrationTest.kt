package duit.server.application.config

import duit.server.domain.bookmark.service.BookmarkedEventIdsCache
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.User
import duit.server.support.CacheConnectionProbe
import duit.server.support.CacheConnectionProbe.CacheCall
import duit.server.support.CacheConnectionProbeConfig
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/**
 * 캐시(Redis) I/O 는 DB 커넥션을 쥔 채로 하지 않는다 (performance/decisions.md D-027).
 *
 * Redis 가 응답을 멈췄을 때(명령마다 타임아웃 1초) 커넥션을 쥔 채 기다리면 풀(10)이 고갈되어
 * Redis 를 쓰지 않는 API 까지 전부 멈췄다(VM 스레드 덤프: 워커 200 중 161 이 커넥션 대기).
 * 로그인 사용자의 캐시 미스 경로는 DB 조회와 캐시 조회·저장이 한 요청에 섞이므로 그 경로들을 본다.
 * v1 목록(GET /api/v1/events)은 운영 트래픽이 없고 MySQL 전용 FIND_IN_SET 을 써 H2 에서 돌릴 수 없어 대상에서 뺐다.
 *
 * 요청 단위의 커넥션 점유를 보려면 테스트 트랜잭션에 합류하면 안 되므로 NOT_SUPPORTED 로 실행하고 픽스처는 직접 지운다.
 */
@Import(CacheConnectionProbeConfig::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("캐시 I/O 는 DB 트랜잭션 밖에서 (D-027)")
class CacheIoOutsideTransactionIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var host: Host
    private lateinit var event: Event
    private lateinit var user: User

    @BeforeEach
    fun setUpCommittedFixtures() {
        transactionTemplate.executeWithoutResult {
            host = TestFixtures.host(name = "캐시 I/O 주최").also(entityManager::persist)
            event = TestFixtures.event(
                title = "캐시 I/O 행사",
                status = EventStatus.RECRUITING,
                statusGroup = EventStatusGroup.ACTIVE,
                host = host,
            ).also(entityManager::persist)
            user = TestFixtures.user(providerId = "cache-io-user", nickname = "캐시IO").also(entityManager::persist)
            entityManager.persist(TestFixtures.bookmark(user = user, event = event))
        }
        CacheConnectionProbe.reset()
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

    @Test
    fun `로그인 행사 목록 캐시 미스 - 목록 조회·저장과 북마크 ID 조회·저장 모두 커넥션 없이`() {
        perform(
            get("/api/v2/events").param("field", "CREATED_AT").param("size", "4").param("statusGroup", "ACTIVE")
        ).andExpect(jsonPath("$.content[?(@.id == ${event.id})].isBookmarked").value(true))

        assertCacheIoWithoutConnection(expected = setOf("get events-5m", "put events-5m", "get $BOOKMARKS", "put $BOOKMARKS"))
    }

    @Test
    fun `로그인 행사 검색 - 캐시를 안 타는 목록도 북마크 ID 캐시는 커넥션 없이`() {
        perform(
            get("/api/v2/events").param("field", "CREATED_AT").param("size", "4").param("searchKeyword", "캐시 I/O")
        ).andExpect(jsonPath("$.content[?(@.id == ${event.id})].isBookmarked").value(true))

        assertCacheIoWithoutConnection(expected = setOf("get $BOOKMARKS", "put $BOOKMARKS"))
    }

    @Test
    fun `로그인 행사 상세 - 행사를 읽은 뒤 북마크 ID 캐시는 커넥션 없이`() {
        perform(get("/api/v2/events/${event.id}"))
            .andExpect(jsonPath("$.host.name").value(host.name))
            .andExpect(jsonPath("$.isBookmarked").value(true))

        assertCacheIoWithoutConnection(expected = setOf("get $BOOKMARKS", "put $BOOKMARKS"))
    }

    private fun perform(request: MockHttpServletRequestBuilder) =
        mockMvc.perform(request.header("Authorization", authHeader(user.id!!))).andExpect(status().isOk)

    /** 기대한 캐시 연산이 실제로 일어났고(프로브 배선 확인), 그 어느 것도 커넥션을 쥔 채가 아니었다 */
    private fun assertCacheIoWithoutConnection(expected: Set<String>) {
        val calls = CacheConnectionProbe.calls()
        val seen = calls.map { "${it.operation} ${it.cacheName}" }.toSet()

        assertTrue(seen.containsAll(expected), "기대한 캐시 연산 $expected, 실제 $seen")
        assertEquals(emptyList<CacheCall>(), calls.filter { it.heldConnections > 0 })
    }

    companion object {
        private const val BOOKMARKS = BookmarkedEventIdsCache.CACHE_NAME
    }
}
