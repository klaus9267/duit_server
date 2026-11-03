package duit.server.domain.bookmark.service

import duit.server.domain.bookmark.repository.BookmarkRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.data.redis.RedisConnectionFailureException

/**
 * Redis 장애 시 북마크 ID 캐시는 DB 조회로 대체한다 (fail-open, D-020).
 * 이 캐시가 생기기 전 행사 상세는 Redis 를 쓰지 않았으므로, 캐시 오류가 요청 실패로 번지면 장애 격리가 후퇴한다.
 */
@DisplayName("BookmarkedEventIdsCache 장애 대체")
class BookmarkedEventIdsCacheTest {

    private val redisDown = RedisConnectionFailureException("Redis 연결 실패")
    private val cache = mockk<Cache>()
    private val bookmarkRepository = mockk<BookmarkRepository> {
        every { findEventIdsByUserId(7L) } returns listOf(1L, 2L)
    }
    private val bookmarkedEventIdsCache = BookmarkedEventIdsCache(
        bookmarkRepository,
        mockk<CacheManager> { every { getCache(BookmarkedEventIdsCache.CACHE_NAME) } returns cache },
    )

    @Test
    fun `캐시 조회가 실패하면 DB 에서 읽은 값을 돌려준다`() {
        every { cache.get(7L) } throws redisDown
        every { cache.put(7L, any()) } throws redisDown

        assertEquals(setOf(1L, 2L), bookmarkedEventIdsCache.get(7L))
    }

    @Test
    fun `캐시 저장이 실패해도 DB 에서 읽은 값을 돌려준다`() {
        every { cache.get(7L) } returns null
        every { cache.put(7L, any()) } throws redisDown

        assertEquals(setOf(1L, 2L), bookmarkedEventIdsCache.get(7L))
    }

    @Test
    fun `무효화가 실패해도 예외를 던지지 않는다`() {
        every { cache.evict(7L) } throws redisDown

        assertDoesNotThrow { bookmarkedEventIdsCache.evictAfterCommit(7L) }
    }
}
