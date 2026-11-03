package duit.server.application.config

import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.core.io.FileSystemResource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureObservability
@DisplayName("Actuator 노출 범위 테스트")
class ActuatorExposureTest : IntegrationTestSupport() {

    companion object {
        // 테스트용 application.yml 이 메인 설정을 가리므로 메인 파일의 실제 값을 읽어 컨텍스트에 적용한다
        private val mainExposure: String = YamlPropertySourceLoader()
            .load("main-application", FileSystemResource("src/main/resources/application.yml"))
            .first()
            .getProperty("management.endpoints.web.exposure.include")
            .toString()

        @JvmStatic
        @DynamicPropertySource
        fun exposure(registry: DynamicPropertyRegistry) {
            registry.add("management.endpoints.web.exposure.include") { mainExposure }
        }

        private val unexposedEndpoints = listOf(
            "heapdump", "threaddump", "env", "configprops", "beans", "mappings", "conditions",
            "loggers", "metrics", "caches", "scheduledtasks", "flyway", "info", "shutdown"
        )
    }

    @Test
    @DisplayName("운영 설정은 와일드카드 없이 health 와 prometheus 만 노출한다")
    fun mainConfigExposesOnlyHealthAndPrometheus() {
        assertEquals("health,prometheus", mainExposure)
    }

    @Test
    @DisplayName("헬스체크는 인증 없이 접근할 수 있고 상세 정보는 숨긴다")
    fun healthIsPublic() {
        // 테스트 환경에는 Redis 가 없어 상태가 DOWN(503)일 수 있다. 여기서는 인증 없이 응답하는지만 본다
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().`is`(anyOf(equalTo(200), equalTo(503))))
            .andExpect(content().string(containsString("\"status\"")))
            .andExpect(content().string(not(containsString("components"))))
    }

    @Test
    @DisplayName("Prometheus 수집 경로는 인증 없이 접근할 수 있다")
    fun prometheusIsPublic() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("jvm_memory_used_bytes")))
    }

    @Test
    @DisplayName("그 밖의 Actuator 엔드포인트는 인증 없이 접근할 수 없다")
    fun otherEndpointsRequireAuthentication() {
        unexposedEndpoints.forEach { endpoint ->
            mockMvc.perform(get("/actuator/$endpoint")).andExpect(status().isUnauthorized)
        }
        mockMvc.perform(post("/actuator/loggers/ROOT")).andExpect(status().isUnauthorized)
        mockMvc.perform(post("/actuator/shutdown")).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("로그인한 사용자에게도 그 밖의 Actuator 엔드포인트는 노출되지 않는다")
    fun otherEndpointsAreNotExposedToAuthenticatedUsers() {
        val user = TestFixtures.user().also(entityManager::persist)
        entityManager.flush()

        unexposedEndpoints.forEach { endpoint ->
            mockMvc.perform(get("/actuator/$endpoint").header("Authorization", authHeader(user.id!!)))
                .andExpect(status().isNotFound)
        }
    }
}
