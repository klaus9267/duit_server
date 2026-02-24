package duit.server.infrastructure.external.firebase

import duit.server.domain.auth.dto.FirebaseTokenClaims
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtValidationException
import java.time.Instant

@DisplayName("FirebaseTokenVerifier 단위 테스트")
class FirebaseTokenVerifierTest {

    private lateinit var firebaseTokenVerifier: FirebaseTokenVerifier
    private lateinit var mockDecoder: JwtDecoder

    @BeforeEach
    fun setUp() {
        firebaseTokenVerifier = FirebaseTokenVerifier(
            projectId = "test-project-id"
        )
        mockDecoder = mockk<JwtDecoder>()
        firebaseTokenVerifier.setDecoder(mockDecoder)
    }

    @Nested
    @DisplayName("verifyIdToken")
    inner class VerifyIdTokenTests {

        @Test
        @DisplayName("유효한 토큰이면 FirebaseTokenClaims를 올바르게 반환한다")
        fun validTokenReturnsCorrectClaims() {
            val token = "valid-jwt-token"
            val now = Instant.now()

            val mockJwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("user-uid-123")
                .claim("email", "test@example.com")
                .claim("name", "Test User")
                .claim("firebase", mapOf("sign_in_provider" to "google.com"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build()

            every { mockDecoder.decode(token) } returns mockJwt

            val result = firebaseTokenVerifier.verifyIdToken(token)

            assertEquals("user-uid-123", result.uid)
            assertEquals("test@example.com", result.email)
            assertEquals("Test User", result.name)
            assertTrue(result.claims.containsKey("firebase"))
        }

        @Test
        @DisplayName("JwtValidationException은 IllegalArgumentException으로 래핑되어 반환된다")
        fun jwtValidationExceptionWrappedAsIllegalArgumentException() {
            val token = "invalid-jwt-token"
            every { mockDecoder.decode(token) } throws JwtValidationException("validation failed", listOf(OAuth2Error("invalid_token")))

            val exception = assertThrows(IllegalArgumentException::class.java) {
                firebaseTokenVerifier.verifyIdToken(token)
            }
            assertTrue(exception.message!!.contains("Firebase 토큰 검증 실패"))
        }

        @Test
        @DisplayName("JwtException은 IllegalArgumentException으로 래핑되어 반환된다")
        fun jwtExceptionWrappedAsIllegalArgumentException() {
            val token = "malformed-jwt-token"
            every { mockDecoder.decode(token) } throws JwtException("Invalid token")

            val exception = assertThrows(IllegalArgumentException::class.java) {
                firebaseTokenVerifier.verifyIdToken(token)
            }
            assertTrue(exception.message!!.contains("유효하지 않은 토큰 형식입니다"))
        }
    }
}
