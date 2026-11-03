package duit.server.application.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.dao.DataAccessException
import org.springframework.dao.QueryTimeoutException
import org.springframework.data.redis.serializer.SerializationException
import java.util.concurrent.Callable

/**
 * Redis 캐시 서킷 브레이커 (performance/decisions.md D-027).
 * Redis 가 응답을 멈추면 명령마다 타임아웃(1초)을 기다리느라 Tomcat 워커가 묶인다. 실패가 쌓이면 일정 시간 Redis 를 부르지 않고
 * 조회는 미스, 저장·삭제는 건너뛴다. 실패 예외 자체는 그대로 던져 기존 fail-open 처리(CacheErrorHandler 등)가 받는다.
 */
@DisplayName("캐시 서킷 브레이커 (D-027)")
class CircuitBreakingCacheTest {

    private val breaker = CircuitBreaker.of(
        "test-cache",
        CircuitBreakerConfig.custom()
            .slidingWindowSize(4)
            .minimumNumberOfCalls(4)
            .failureRateThreshold(50f)
            .permittedNumberOfCallsInHalfOpenState(1)
            .recordExceptions(DataAccessException::class.java)
            .build()
    )
    private val redis = FlakyCache(ConcurrentMapCache("events-5m"))
    private val cache = CircuitBreakingCacheManager.CircuitBreakingCache(redis, breaker)

    @Test
    fun `닫혀 있으면 그대로 위임한다`() {
        cache.put("k", "v")

        assertEquals("v", cache.get("k")?.get())
        assertEquals(2, redis.calls)
    }

    @Test
    fun `닫혀 있을 때 Redis 오류는 그대로 던진다 - fail-open 은 호출자 몫`() {
        redis.failWith = QueryTimeoutException("Redis command timed out")

        assertThrows<QueryTimeoutException> { cache.get("k") }
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state)
    }

    @Test
    fun `실패가 기준을 넘으면 열리고 그동안 Redis 를 부르지 않는다`() {
        tripOpen()
        val callsWhenOpened = redis.calls

        assertNull(cache.get("k"))                       // 조회 → 미스
        cache.put("k", "v")                              // 저장 → 건너뜀
        cache.evict("k")                                 // 삭제 → 건너뜀
        cache.clear()                                    // 비우기 → 건너뜀
        assertEquals("loaded", cache.get("k") { "loaded" }) // 동기 조회 → 원본 계산만

        assertEquals(callsWhenOpened, redis.calls)
        assertEquals(CircuitBreaker.State.OPEN, breaker.state)
    }

    @Test
    fun `반열림에서 시험 호출이 성공하면 다시 닫힌다`() {
        tripOpen()
        redis.failWith = null
        breaker.transitionToHalfOpenState()

        cache.put("k", "v")

        assertEquals(CircuitBreaker.State.CLOSED, breaker.state)
        assertEquals("v", cache.get("k")?.get())
    }

    @Test
    fun `직렬화 오류는 Redis 장애가 아니므로 세지 않는다`() {
        redis.failWith = SerializationException("bad payload")

        repeat(8) { runCatching { cache.get("k") } }

        assertEquals(CircuitBreaker.State.CLOSED, breaker.state)
    }

    private fun tripOpen() {
        redis.failWith = QueryTimeoutException("Redis command timed out")
        repeat(4) { runCatching { cache.get("k") } }
        assertEquals(CircuitBreaker.State.OPEN, breaker.state)
    }

    /** 호출 수를 세고, 지정한 예외로 실패하는 캐시 (멈춘 Redis 대역) */
    private class FlakyCache(private val delegate: Cache) : Cache by delegate {
        var failWith: RuntimeException? = null
        var calls = 0

        private fun <T> call(block: () -> T): T {
            calls++
            failWith?.let { throw it }
            return block()
        }

        override fun get(key: Any): Cache.ValueWrapper? = call { delegate.get(key) }
        override fun put(key: Any, value: Any?) = call { delegate.put(key, value) }
        override fun evict(key: Any) = call { delegate.evict(key) }
        override fun clear() = call { delegate.clear() }
        override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? = call { delegate.get(key, valueLoader) }
    }
}
