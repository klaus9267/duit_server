package duit.server.application.config

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * 운영 URL 에만 Connector/J 준비문 캐시가 빠져 있어 SQL 마다 드라이버가 문자열을 다시 파싱하던 설정 드리프트를 막는다.
 * (A-1 이후 JFR 에서 요청 스레드 CPU 의 9.3% — performance/journal.md 측정 ⑨, decisions.md D-019)
 */
@DisplayName("운영 ↔ 로컬 DB URL 일치")
class DataSourceUrlParityTest {

    @Test
    @DisplayName("운영 DB URL 은 로컬과 같은 준비문 캐시 옵션을 쓴다")
    fun preparedStatementCacheMatchesLocal() {
        val prod = queryParams("src/main/resources/application-prod.yml")
        val local = queryParams("src/main/resources/application-local.yml")

        assertAll(CACHE_KEYS.map { key ->
            Executable {
                assertNotNull(prod[key], "운영 URL 에 $key 가 없다")
                assertEquals(local[key], prod[key], key)
            }
        })
    }

    @Suppress("UNCHECKED_CAST")
    private fun queryParams(path: String): Map<String, String> {
        val yaml: Map<String, Any?> = Yaml().load(File(path).readText())
        val datasource = (yaml["spring"] as Map<String, Any?>)["datasource"] as Map<String, Any?>
        return (datasource["url"] as String).substringAfter('?').split('&')
            .associate { it.substringBefore('=') to it.substringAfter('=') }
    }

    companion object {
        private val CACHE_KEYS = listOf("cachePrepStmts", "prepStmtCacheSize", "prepStmtCacheSqlLimit")
    }
}
