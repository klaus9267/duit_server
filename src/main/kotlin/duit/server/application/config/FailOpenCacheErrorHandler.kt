package duit.server.application.config

import duit.server.application.common.ThrottledLogger
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler

/**
 * 캐시(Redis) 오류를 요청 실패로 번지게 하지 않는다 (performance/decisions.md D-021, H-08).
 * - 조회 실패 → 캐시 미스로 처리돼 메서드(DB 조회)가 실행된다.
 * - 저장 실패 → 버린다. 다음 요청이 다시 DB 를 본다.
 * - 삭제·비우기 실패 → 버린다. Redis 가 데이터를 잃지 않은 채 돌아오면 옛 값이 TTL(3·5분) 동안 남을 수 있다.
 * 기본 처리기(SimpleCacheErrorHandler)는 예외를 그대로 던져, Redis 가 죽으면 캐시를 쓰는 목록 요청이 전부 500 이 됐다.
 */
class FailOpenCacheErrorHandler : CacheErrorHandler {

    private val throttled = ThrottledLogger(LoggerFactory.getLogger(javaClass))

    override fun handleCacheGetError(exception: RuntimeException, cache: Cache, key: Any) =
        throttled.warn("캐시 조회 실패 → DB 조회로 대체 (${cache.name})", exception)

    override fun handleCachePutError(exception: RuntimeException, cache: Cache, key: Any, value: Any?) =
        throttled.warn("캐시 저장 실패 → 버림 (${cache.name})", exception)

    override fun handleCacheEvictError(exception: RuntimeException, cache: Cache, key: Any) =
        throttled.warn("캐시 삭제 실패 → 옛 값이 TTL 동안 남을 수 있음 (${cache.name})", exception)

    override fun handleCacheClearError(exception: RuntimeException, cache: Cache) =
        throttled.warn("캐시 비우기 실패 → 옛 값이 TTL 동안 남을 수 있음 (${cache.name})", exception)
}
