package duit.server.support

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

/**
 * Hikari 와 JDBC 드라이버 사이에서 Connection 메서드 호출 수를 센다.
 *
 * MySQL Connector/J 에서 setReadOnly / setAutoCommit / commit / rollback 은 각각
 * `SET SESSION TRANSACTION …` / `SET autocommit=…` / `COMMIT` / `ROLLBACK` 문장 1개로 나간다.
 * 그래서 H2 테스트에서도 운영 DB 왕복 수를 1:1 로 환산할 수 있다. (H2 는 이 호출들을 SQL 로 보내지 않는다)
 *
 * 순차 실행 전제의 전역 카운터. 측정 직전에 [reset] 한다.
 */
object JdbcCallCounter {
    private val COUNTED = setOf("setAutoCommit", "setReadOnly", "commit", "rollback", "prepareStatement", "createStatement", "prepareCall")
    private val counts = ConcurrentHashMap<String, AtomicInteger>()

    fun reset() = counts.clear()

    fun snapshot(): Map<String, Int> = counts.mapValues { it.value.get() }.filterValues { it > 0 }

    internal fun record(method: String) {
        if (method in COUNTED) counts.computeIfAbsent(method) { AtomicInteger() }.incrementAndGet()
    }
}

/**
 * Hikari 가 드라이버에서 물리 커넥션을 받는 지점에 [JdbcCallCounter] 를 끼운다.
 * 풀이 시작되기 전(빈 초기화 직후)에 `dataSource` 를 지정하므로 jdbcUrl 대신 이 DataSource 가 쓰인다.
 */
@TestConfiguration
class JdbcCallCounterConfig {

    companion object {
        @Bean
        @JvmStatic
        fun jdbcCallCountingPostProcessor(): BeanPostProcessor = object : BeanPostProcessor {
            override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
                if (bean is HikariDataSource && bean.dataSource == null) {
                    val driver = DriverManagerDataSource(bean.jdbcUrl, bean.username, bean.password)
                        .apply { bean.driverClassName?.let(::setDriverClassName) }
                    bean.dataSource = countingDataSource(driver)
                }
                return bean
            }
        }

        private fun countingDataSource(target: DataSource): DataSource =
            delegatingProxy(target, DataSource::class.java) { method, result ->
                if (method == "getConnection") countingConnection(result as Connection) else result
            }

        private fun countingConnection(target: Connection): Connection =
            delegatingProxy(target, Connection::class.java, before = JdbcCallCounter::record) { _, result -> result }

        private fun <T : Any> delegatingProxy(
            target: T,
            type: Class<T>,
            before: (String) -> Unit = {},
            after: (String, Any?) -> Any?,
        ): T = type.cast(
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
                before(method.name)
                try {
                    after(method.name, method.invoke(target, *(args ?: emptyArray())))
                } catch (e: InvocationTargetException) {
                    throw e.targetException
                }
            }
        )
    }
}
