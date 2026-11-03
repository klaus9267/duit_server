package duit.server.application.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private val secret = "testSecretKeyForUnitTestingOnly1234567890testSecretKeyForUnitTestingOnly"
    private val provider = JwtTokenProvider(secret, 3_600_000)

    @Test
    fun `유효한 토큰이면 userId 를 주체로 하는 인증 객체를 만든다`() {
        val authentication = provider.getAuthenticationOrNull(provider.createAccessToken(42L))

        assertEquals(42L, authentication?.principal)
        assertEquals(listOf("ROLE_USER"), authentication?.authorities?.map { it.authority })
    }

    @Test
    fun `다른 키로 서명한 토큰은 인증하지 않는다`() {
        val forged = JwtTokenProvider(secret.reversed(), 3_600_000).createAccessToken(42L)

        assertNull(provider.getAuthenticationOrNull(forged))
    }

    @Test
    fun `만료된 토큰은 인증하지 않는다`() {
        val expired = JwtTokenProvider(secret, -1_000).createAccessToken(42L)

        assertNull(provider.getAuthenticationOrNull(expired))
    }

    @Test
    fun `형식이 잘못된 토큰은 인증하지 않는다`() {
        assertNull(provider.getAuthenticationOrNull("not-a-jwt"))
        assertNull(provider.getAuthenticationOrNull(""))
    }
}
