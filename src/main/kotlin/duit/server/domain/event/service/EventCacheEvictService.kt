package duit.server.domain.event.service

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service

/**
 * 행사 캐시 전체 무효화 서비스
 *
 * 행사 생성/수정/삭제 또는 스케줄러 상태 업데이트 시 events-5m, events-3m 캐시를 모두 비운다.
 */
@Service
class EventCacheEvictService {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Caching(
        evict = [
            CacheEvict(cacheNames = ["events-5m"], allEntries = true),
            CacheEvict(cacheNames = ["events-3m"], allEntries = true)
        ]
    )
    fun evictAll() {
        logger.info("Event caches evicted (events-5m, events-3m)")
    }
}
