package duit.server.application.config

import duit.server.domain.bookmark.service.BookmarkedEventIdsCache
import duit.server.domain.host.service.HostService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

/**
 * 테스트 환경용 캐시 설정
 * Redis 없이 ConcurrentMapCache로 동작. 운영과 같이 서킷 브레이커로 감싼다 (D-027)
 */
@Configuration
@EnableCaching
@Profile("test")
class TestCacheConfig {

    @Bean
    @Primary
    fun cacheManager(cacheCircuitBreaker: CircuitBreaker): CacheManager {
        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(
            listOf(
                ConcurrentMapCache("events-5m"),
                ConcurrentMapCache("events-3m"),
                ConcurrentMapCache(BookmarkedEventIdsCache.CACHE_NAME),
                ConcurrentMapCache(HostService.HOSTS_CACHE)
            )
        )
        cacheManager.afterPropertiesSet() // 감싸면 빈이 아니라 Spring 이 초기화해 주지 않는다
        return CircuitBreakingCacheManager(cacheManager, cacheCircuitBreaker)
    }
}
