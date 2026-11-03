package duit.server.application.config

import duit.server.support.IntegrationTestSupport
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheInterceptor
import org.springframework.data.redis.RedisConnectionFailureException

/**
 * C-2: 캐시(Redis) 오류가 요청 실패(500)로 번지지 않는다 (performance/decisions.md D-021, H-08).
 */
@DisplayName("캐시 오류 fail-open (C-2)")
class FailOpenCacheErrorHandlerTest {

    private val handler = FailOpenCacheErrorHandler()
    private val cache = mockk<Cache>(relaxed = true)
    private val redisDown = RedisConnectionFailureException("Redis 연결 실패")

    @Test
    fun `조회·저장·삭제·비우기 오류를 삼킨다 — 조회 실패는 캐시 미스로 처리돼 DB 에서 읽는다`() {
        assertAll(
            { assertDoesNotThrow { handler.handleCacheGetError(redisDown, cache, "key") } },
            { assertDoesNotThrow { handler.handleCachePutError(redisDown, cache, "key", "value") } },
            { assertDoesNotThrow { handler.handleCacheEvictError(redisDown, cache, "key") } },
            { assertDoesNotThrow { handler.handleCacheClearError(redisDown, cache) } },
        )
    }

    @Nested
    @DisplayName("연결")
    inner class Wiring : IntegrationTestSupport() {

        @Autowired
        private lateinit var cacheInterceptor: CacheInterceptor

        @Test
        fun `@Cacheable 처리기가 fail-open 오류 처리기를 쓴다`() {
            assertInstanceOf(FailOpenCacheErrorHandler::class.java, cacheInterceptor.errorHandler)
        }
    }
}
