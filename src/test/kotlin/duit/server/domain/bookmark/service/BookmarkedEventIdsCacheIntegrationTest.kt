package duit.server.domain.bookmark.service

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/**
 * A-2 북마크 ID 캐시의 정합성 (performance/decisions.md D-020).
 * 무효화가 커밋 뒤에 일어나는지 보려면 실제 커밋이 필요하므로 테스트 트랜잭션 없이(NOT_SUPPORTED) 돌리고 픽스처를 직접 지운다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("북마크 ID 캐시 (A-2)")
class BookmarkedEventIdsCacheIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var bookmarkedEventIdsCache: BookmarkedEventIdsCache

    @Autowired
    private lateinit var cacheManager: CacheManager

    private lateinit var host: Host
    private lateinit var event: Event
    private lateinit var user: User

    @BeforeEach
    fun setUpCommittedFixtures() {
        transactionTemplate.executeWithoutResult {
            host = TestFixtures.host(name = "북마크 캐시 주최").also(entityManager::persist)
            event = TestFixtures.event(
                title = "북마크 캐시 행사",
                status = EventStatus.RECRUITING,
                statusGroup = EventStatusGroup.ACTIVE,
                host = host,
            ).also(entityManager::persist)
            user = TestFixtures.user(providerId = "bookmark-cache-user", nickname = "북마크캐시").also(entityManager::persist)
        }
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
    fun `북마크를 토글하면 캐시된 목록에서도 바로 바뀐다`() {
        listActiveEvents().andExpect(bookmarked(false)) // 북마크 없음이 캐시된 상태

        toggleBookmark()
        listActiveEvents().andExpect(bookmarked(true))

        toggleBookmark()
        listActiveEvents().andExpect(bookmarked(false))
    }

    @Test
    fun `행사 상세도 같은 캐시로 북마크 여부를 낸다`() {
        mockMvc.perform(get("/api/v2/events/${event.id}").header("Authorization", authHeader(user.id!!)))
            .andExpect(jsonPath("$.isBookmarked").value(false))

        toggleBookmark()

        mockMvc.perform(get("/api/v2/events/${event.id}").header("Authorization", authHeader(user.id!!)))
            .andExpect(jsonPath("$.isBookmarked").value(true))
    }

    @Test
    fun `북마크가 없는 사용자도 빈 목록으로 캐시한다`() {
        assertEquals(emptySet<Long>(), bookmarkedEventIdsCache.get(user.id!!))

        assertNotNull(cache().get(user.id!!), "빈 목록도 캐시돼야 다음 요청이 DB 를 안 본다")
    }

    @Test
    fun `무효화는 트랜잭션이 커밋된 뒤에 일어난다`() {
        bookmarkedEventIdsCache.get(user.id!!)

        transactionTemplate.executeWithoutResult {
            bookmarkedEventIdsCache.evictAfterCommit(user.id!!)
            // 커밋 전에 지우면, 그 사이 다른 요청이 커밋 전 DB 를 읽어 옛 값을 다시 채울 수 있다
            assertNotNull(cache().get(user.id!!), "커밋 전에는 남아 있어야 한다")
        }

        assertNull(cache().get(user.id!!))
    }

    @Test
    fun `롤백된 트랜잭션은 캐시를 지우지 않는다`() {
        bookmarkedEventIdsCache.get(user.id!!)

        transactionTemplate.executeWithoutResult { tx ->
            bookmarkedEventIdsCache.evictAfterCommit(user.id!!)
            tx.setRollbackOnly()
        }

        assertNotNull(cache().get(user.id!!))
    }

    private fun cache() = cacheManager.getCache(BookmarkedEventIdsCache.CACHE_NAME)!!

    private fun toggleBookmark() {
        mockMvc.perform(post("/api/v1/bookmarks/${event.id}").header("Authorization", authHeader(user.id!!)))
            .andExpect(status().isOk)
    }

    private fun listActiveEvents(): ResultActions =
        mockMvc.perform(
            get("/api/v2/events")
                .param("field", "CREATED_AT")
                .param("size", "4")
                .param("statusGroup", "ACTIVE")
                .header("Authorization", authHeader(user.id!!))
        ).andExpect(status().isOk)

    private fun bookmarked(expected: Boolean) =
        jsonPath("$.content[?(@.id == ${event.id})].isBookmarked").value(expected)
}
