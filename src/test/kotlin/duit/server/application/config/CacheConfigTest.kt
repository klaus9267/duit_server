package duit.server.application.config

import duit.server.domain.bookmark.service.BookmarkedEventIdsCache
import duit.server.domain.host.service.HostService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** 운영 캐시 설정(@Profile("!test"))은 테스트 컨텍스트에 안 뜨므로 직접 만들어 배선을 본다 (D-027) */
@DisplayName("운영 캐시 매니저 배선")
class CacheConfigTest {

    @Test
    fun `설정한 캐시 4개를 모두 서킷 브레이커로 감싸 내놓는다`() {
        val manager = CacheConfig(mockk(relaxed = true), CircuitBreaker.ofDefaults("test")).cacheManager()

        // 감싼 RedisCacheManager 를 초기화하지 않으면 캐시별 설정이 안 실려 이름 목록이 비고, 기본값 캐시가 즉석에서 생긴다
        assertEquals(
            setOf("events-5m", "events-3m", BookmarkedEventIdsCache.CACHE_NAME, HostService.HOSTS_CACHE),
            manager.cacheNames.toSet(),
        )
        manager.cacheNames.forEach { assertInstanceOf(CircuitBreakingCacheManager.CircuitBreakingCache::class.java, manager.getCache(it)) }
    }
}
