package duit.server.application.config

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

/**
 * 모든 캐시 호출을 서킷 브레이커 하나로 감싼다 — 캐시는 전부 같은 Redis 하나라 장애도 하나다 (performance/decisions.md D-027).
 *
 * Redis 가 응답을 멈추면(연결은 열린 채 무응답) 명령마다 타임아웃(1초)을 기다린다. 목록 요청 하나가 조회·저장으로 2초씩 묶이면
 * 500 RPS 에서 Tomcat 워커 200 이 모자라 Redis 를 안 쓰는 API 까지 멈춘다. 실패가 쌓이면 브레이커가 열리고, 열린 동안은
 * Redis 를 부르지 않는다: 조회 = 미스(DB 조회), 저장·삭제·비우기 = 건너뜀.
 *
 * 닫힌 상태의 Redis 오류는 그대로 던진다. 요청 실패로 번지지 않게 하는 일은 기존 fail-open 처리
 * (FailOpenCacheErrorHandler, BookmarkedEventIdsCache)가 그대로 맡는다.
 */
class CircuitBreakingCacheManager(
    private val delegate: CacheManager,
    private val breaker: CircuitBreaker,
) : CacheManager {
    private val caches = ConcurrentHashMap<String, Cache>()

    override fun getCache(name: String): Cache? =
        delegate.getCache(name)?.let { target -> caches.computeIfAbsent(name) { CircuitBreakingCache(target, breaker) } }

    override fun getCacheNames(): Collection<String> = delegate.cacheNames

    class CircuitBreakingCache(
        private val delegate: Cache,
        private val breaker: CircuitBreaker,
    ) : Cache {
        /** 브레이커가 허락하지 않으면(열림, 반열림의 시험 호출 초과) Redis 를 부르지 않고 [skipped] 를 돌려준다 */
        private fun <T> guarded(skipped: () -> T, call: () -> T): T =
            try {
                breaker.executeSupplier(call)
            } catch (e: CallNotPermittedException) {
                skipped()
            }

        override fun getName(): String = delegate.name
        override fun getNativeCache(): Any = delegate.nativeCache

        override fun get(key: Any): Cache.ValueWrapper? = guarded({ null }) { delegate.get(key) }
        override fun <T : Any?> get(key: Any, type: Class<T>?): T? = guarded({ null }) { delegate.get(key, type) }
        override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? =
            guarded({ valueLoader.call() }) { delegate.get(key, valueLoader) }

        override fun put(key: Any, value: Any?) = guarded({}) { delegate.put(key, value) }
        override fun putIfAbsent(key: Any, value: Any?): Cache.ValueWrapper? = guarded({ null }) { delegate.putIfAbsent(key, value) }
        override fun evict(key: Any) = guarded({}) { delegate.evict(key) }
        override fun evictIfPresent(key: Any): Boolean = guarded({ false }) { delegate.evictIfPresent(key) }
        override fun clear() = guarded({}) { delegate.clear() }
        override fun invalidate(): Boolean = guarded({ false }) { delegate.invalidate() }
    }
}
