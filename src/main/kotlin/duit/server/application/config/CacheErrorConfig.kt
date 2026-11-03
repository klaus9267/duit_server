package duit.server.application.config

import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Configuration

/** @Cacheable·@CacheEvict 의 캐시 오류 처리 (운영·테스트 공통). 캐시 매니저 자체는 CacheConfig / TestCacheConfig */
@Configuration
class CacheErrorConfig : CachingConfigurer {
    override fun errorHandler(): CacheErrorHandler = FailOpenCacheErrorHandler()
}
