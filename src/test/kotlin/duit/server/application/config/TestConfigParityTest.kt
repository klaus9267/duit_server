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
 * src/test/resources/application.yml 이 클래스패스에서 main 의 application.yml 을 가리므로,
 * 커넥션·트랜잭션·응답 직렬화 동작을 좌우하는 설정은 application-test.yml 에 복제돼 있어야 테스트가 운영과 같은 모드로 돈다.
 * (가려진 탓에 테스트만 OSIV 가 켜져 있던 적이 있다 — performance/journal.md A-1)
 */
@DisplayName("테스트 설정 ↔ 운영 설정 일치")
class TestConfigParityTest {

    @Test
    @DisplayName("커넥션·트랜잭션·응답 직렬화 설정은 application.yml 과 application-test.yml 이 같다")
    fun connectionSettingsMatchProduction() {
        val main = flatten(load("src/main/resources/application.yml"))
        val test = flatten(load("src/test/resources/application-test.yml"))

        assertAll(PARITY_KEYS.map { key ->
            Executable {
                assertNotNull(main[key], "운영 application.yml 에 $key 가 없다")
                assertEquals(main[key], test[key], key)
            }
        })
    }

    private fun load(path: String): Map<String, Any?> = Yaml().load(File(path).readText())

    private fun flatten(map: Map<String, Any?>, prefix: String = ""): Map<String, Any?> =
        map.flatMap { (key, value) ->
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            @Suppress("UNCHECKED_CAST")
            if (value is Map<*, *>) flatten(value as Map<String, Any?>, path).toList() else listOf(path to value)
        }.toMap()

    companion object {
        private val PARITY_KEYS = listOf(
            "spring.jpa.open-in-view",
            "spring.jpa.properties.hibernate.connection.handling_mode",
            "spring.datasource.hikari.auto-commit",
            "spring.datasource.hikari.maximum-pool-size",
            "spring.datasource.hikari.minimum-idle",
            "spring.datasource.hikari.connection-timeout",
            "spring.jackson.serialization.indent-output",
        )
    }
}
