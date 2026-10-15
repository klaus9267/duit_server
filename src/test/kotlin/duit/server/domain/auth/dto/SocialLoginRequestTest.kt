package duit.server.domain.auth.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("SocialLoginRequest 파싱 테스트")
class SocialLoginRequestTest {

    @Test
    fun `구버전 클라이언트가 보낸 ID 토큰 원문을 그대로 사용한다`() {
        val request = SocialLoginRequest.parse("  eyJhbGciOiJSUzI1NiJ9.payload.signature \n")

        assertEquals("eyJhbGciOiJSUzI1NiJ9.payload.signature", request.idToken)
        assertNull(request.agreedTermsVersion)
    }

    @Test
    fun `JSON 본문에서 ID 토큰과 약관 버전을 읽는다`() {
        val request = SocialLoginRequest.parse("""{"idToken":"token-1","agreedTermsVersion":"2026-10-15"}""")

        assertEquals("token-1", request.idToken)
        assertEquals("2026-10-15", request.agreedTermsVersion)
    }

    @Test
    fun `약관 버전 없이 보낸 JSON 본문도 허용한다`() {
        val request = SocialLoginRequest.parse("""{"idToken":"token-1"}""")

        assertEquals("token-1", request.idToken)
        assertNull(request.agreedTermsVersion)
    }

    @Test
    fun `빈 약관 버전은 보내지 않은 것으로 본다`() {
        listOf("", "   ").forEach { version ->
            val request = SocialLoginRequest.parse("""{"idToken":"token-1","agreedTermsVersion":"$version"}""")

            assertEquals("token-1", request.idToken)
            assertNull(request.agreedTermsVersion)
        }
    }

    @Test
    fun `약관 버전이 yyyy-MM-dd 형식이 아니면 거부한다`() {
        listOf("20261015", "2026-13-01", "2026-02-30", "v2026-10-15", "2026-10-15-extra-long-version")
            .forEach { version ->
                val exception = assertThrows<IllegalArgumentException> {
                    SocialLoginRequest.parse("""{"idToken":"token-1","agreedTermsVersion":"$version"}""")
                }
                assertEquals("약관 버전은 yyyy-MM-dd 형식이어야 합니다", exception.message)
            }
    }

    @Test
    fun `ID 토큰이 없거나 JSON 이 깨졌으면 거부한다`() {
        listOf("""{"agreedTermsVersion":"2026-10-15"}""", """{"idToken":""", """{"idToken":null}""")
            .forEach { body ->
                assertThrows<IllegalArgumentException> { SocialLoginRequest.parse(body) }
            }
    }
}
