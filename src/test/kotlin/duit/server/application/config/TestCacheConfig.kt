package duit.server.application.config

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
 * Redis 없이 ConcurrentMapCache로 동작
 */
@Configuration
@EnableCaching
@Profile("test")
class TestCacheConfig {

    @Bean
    @Primary
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(
            listOf(
                ConcurrentMapCache("events-5m"),
                ConcurrentMapCache("events-3m")
            )
        )
        return cacheManager
    }
}
