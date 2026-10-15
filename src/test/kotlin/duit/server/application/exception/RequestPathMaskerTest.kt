package duit.server.application.exception

import duit.server.infrastructure.external.discord.DiscordService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

@DisplayName("요청 경로 마스킹 테스트")
class RequestPathMaskerTest {

    @Test
    fun `디바이스 토큰 경로 변수를 가린다`() {
        assertEquals(
            "/api/v1/users/device/***",
            RequestPathMasker.mask("/api/v1/users/device/dGVzdC1mY20tdG9rZW46QVBBOTFiRw-_x"),
        )
        assertEquals("/api/v1/users/device/***/", RequestPathMasker.mask("/api/v1/users/device/token-1/"))
    }

    @Test
    fun `민감 정보가 없는 경로는 그대로 둔다`() {
        listOf("/api/v1/users/me", "/api/v1/users/device", "/api/v2/events/10").forEach { path ->
            assertEquals(path, RequestPathMasker.mask(path))
        }
    }

    @Test
    fun `5xx 오류를 Discord 로 보낼 때 디바이스 토큰을 가린다`() {
        val discordService = mockk<DiscordService>(relaxed = true)
        val handler = GlobalExceptionHandler(discordService, activeProfile = "prod")
        val request = MockHttpServletRequest("PATCH", "/api/v1/users/device/secret-fcm-token")

        val response = handler.handleGenericException(RuntimeException("boom"), request)

        assertEquals("/api/v1/users/device/***", response.body!!.path)
        verify(exactly = 1) {
            discordService.sendServerErrorNotification(
                errorCode = any(),
                message = any(),
                path = "/api/v1/users/device/***",
                timestamp = any(),
                exception = any(),
            )
        }
    }
}
