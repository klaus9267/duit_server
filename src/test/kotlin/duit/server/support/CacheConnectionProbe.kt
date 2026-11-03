package duit.server.support

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.sql.DataSource

/**
 * 캐시 연산마다 그 순간 풀에서 빌려 간 DB 커넥션 수를 기록한다 (performance/decisions.md D-027).
 *
 * 운영 캐시는 원격(Redis)이라, 커넥션을 쥔 채 캐시를 부르면 Redis 가 느려지는 만큼 커넥션도 묶인다.
 * 테스트 캐시(ConcurrentMapCache)는 즉시 응답하므로 지연이 아니라 "그때 커넥션을 쥐고 있었나"로 본다.
 *
 * 순차 실행 전제의 전역 기록. 측정 직전에 [reset] 한다.
 */
object CacheConnectionProbe {
    data class CacheCall(val operation: String, val cacheName: String, val heldConnections: Int)

    private val calls = CopyOnWriteArrayList<CacheCall>()

    fun reset() = calls.clear()

    fun calls(): List<CacheCall> = calls.toList()

    internal fun record(call: CacheCall) {
        calls += call
    }
}

/** `cacheManager` 빈을 [CacheConnectionProbe] 에 기록하는 캐시 매니저로 감싼다. */
@TestConfiguration
class CacheConnectionProbeConfig {

    companion object {
        @Bean
        @JvmStatic
        fun cacheConnectionProbePostProcessor(dataSource: ObjectProvider<DataSource>): BeanPostProcessor =
            object : BeanPostProcessor {
                override fun postProcessAfterInitialization(bean: Any, beanName: String): Any =
                    if (bean is CacheManager) ProbingCacheManager(bean) { heldConnections(dataSource) } else bean
            }

        private fun heldConnections(dataSource: ObjectProvider<DataSource>): Int =
            dataSource.getObject().unwrap(HikariDataSource::class.java).hikariPoolMXBean.activeConnections
    }
}

private class ProbingCacheManager(
    private val delegate: CacheManager,
    private val heldConnections: () -> Int,
) : CacheManager {
    private val caches = ConcurrentHashMap<String, Cache>()

    override fun getCache(name: String): Cache? =
        delegate.getCache(name)?.let { target -> caches.computeIfAbsent(name) { ProbingCache(target, heldConnections) } }

    override fun getCacheNames(): Collection<String> = delegate.cacheNames
}

private class ProbingCache(
    private val delegate: Cache,
    private val heldConnections: () -> Int,
) : Cache {
    private fun <T> probe(operation: String, call: () -> T): T {
        CacheConnectionProbe.record(CacheConnectionProbe.CacheCall(operation, delegate.name, heldConnections()))
        return call()
    }

    override fun getName(): String = delegate.name
    override fun getNativeCache(): Any = delegate.nativeCache
    override fun get(key: Any): Cache.ValueWrapper? = probe("get") { delegate.get(key) }
    override fun <T : Any?> get(key: Any, type: Class<T>?): T? = probe("get") { delegate.get(key, type) }
    override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? = probe("get") { delegate.get(key, valueLoader) }
    override fun put(key: Any, value: Any?) = probe("put") { delegate.put(key, value) }
    override fun putIfAbsent(key: Any, value: Any?): Cache.ValueWrapper? = probe("put") { delegate.putIfAbsent(key, value) }
    override fun evict(key: Any) = probe("evict") { delegate.evict(key) }
    override fun evictIfPresent(key: Any): Boolean = probe("evict") { delegate.evictIfPresent(key) }
    override fun clear() = probe("clear") { delegate.clear() }
    override fun invalidate(): Boolean = probe("clear") { delegate.invalidate() }
}
