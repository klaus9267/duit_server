package duit.server.application.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MetricsTokenAuthorizationManager 단위 테스트")
class MetricsTokenAuthorizationManagerTest {

    private val manager = MetricsTokenAuthorizationManager("metrics-secret")

    @Test
    fun `일치하는 Bearer 토큰이면 허용한다`() {
        assertTrue(manager.isValid("Bearer metrics-secret"))
    }

    @Test
    fun `토큰이 없거나 다르면 거부한다`() {
        listOf(null, "", "Bearer ", "Bearer wrong", "Bearer metrics-secret-x", "Basic metrics-secret", "metrics-secret")
            .forEach { header -> assertFalse(manager.isValid(header), "header=$header") }
    }

    @Test
    fun `설정된 토큰이 비어 있으면 모든 요청을 거부한다`() {
        listOf("", "   ").forEach { configured ->
            val blankManager = MetricsTokenAuthorizationManager(configured)
            assertFalse(blankManager.isValid("Bearer "))
            assertFalse(blankManager.isValid("Bearer $configured"))
            assertFalse(blankManager.isValid(null))
        }
    }
}
