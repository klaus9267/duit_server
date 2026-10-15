package duit.server.application.config

import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.core.io.FileSystemResource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

// 테스트 클래스패스의 application.yml 이 main 설정을 가리므로 운영과 같은 노출 범위를 직접 지정한다.
// Redis 는 목 객체라 헬스 인디케이터만 끈다.
@AutoConfigureObservability
@TestPropertySource(
    properties = [
        "metrics.token=test-metrics-token",
        "management.endpoints.web.exposure.include=health,prometheus",
        "management.endpoints.web.discovery.enabled=false",
        "management.health.redis.enabled=false",
    ]
)
@DisplayName("Actuator 접근 제어 통합 테스트")
class ActuatorSecurityIntegrationTest : IntegrationTestSupport() {

    @Test
    fun `운영 설정은 health 와 prometheus 만 노출한다`() {
        val properties = YamlPropertiesFactoryBean().apply {
            setResources(FileSystemResource("src/main/resources/application.yml"))
        }.getObject()!!

        assertEquals("health,prometheus", properties.getProperty("management.endpoints.web.exposure.include"))
        assertEquals("false", properties.getProperty("management.endpoints.web.discovery.enabled"))
        assertEquals("\${METRICS_TOKEN:}", properties.getProperty("metrics.token"))
    }

    @Test
    fun `헬스체크는 인증 없이 200을 반환한다`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `env 등 민감한 엔드포인트는 노출되지 않는다`() {
        val user = TestFixtures.user(providerId = "actuator-user").also(entityManager::persist)
        entityManager.flush()

        listOf("/actuator/env", "/actuator/configprops", "/actuator/beans", "/actuator/loggers").forEach { path ->
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized)
            mockMvc.perform(get(path).header("Authorization", authHeader(user.id!!)))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    fun `엔드포인트 목록 페이지는 회원에게도 노출하지 않는다`() {
        val user = TestFixtures.user(providerId = "actuator-discovery-user").also(entityManager::persist)
        entityManager.flush()

        mockMvc.perform(get("/actuator").header("Authorization", authHeader(user.id!!)))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `prometheus 는 토큰이 없거나 틀리면 거부한다`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer wrong-token"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `회원 JWT 로는 prometheus 에 접근할 수 없다`() {
        val user = TestFixtures.user(providerId = "actuator-jwt-user").also(entityManager::persist)
        entityManager.flush()

        mockMvc.perform(get("/actuator/prometheus").header("Authorization", authHeader(user.id!!)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `prometheus 는 올바른 토큰이면 메트릭을 반환한다`() {
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer test-metrics-token"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("jvm_")))
    }
}
