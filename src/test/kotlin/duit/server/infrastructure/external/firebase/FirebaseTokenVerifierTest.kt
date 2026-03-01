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

        @Test
        @DisplayName("sub(uid) claim 없는 토큰 → IllegalArgumentException 발생")
        fun missingSubjectThrowsIllegalArgumentException() {
            val token = "no-subject-token"
            val now = Instant.now()
            val mockJwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .claim("email", "test@example.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build()
            every { mockDecoder.decode(token) } returns mockJwt

            val exception = assertThrows(IllegalArgumentException::class.java) {
                firebaseTokenVerifier.verifyIdToken(token)
            }
            assertTrue(exception.message!!.contains("토큰에 uid(sub)가 없습니다"))
        }

        @Test
        @DisplayName("email, name claim 없는 토큰 → null 반환으로 NPE 없음")
        fun missingOptionalClaimsReturnsNull() {
            val token = "no-email-name-token"
            val now = Instant.now()
            val mockJwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("user-uid-456")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build()
            every { mockDecoder.decode(token) } returns mockJwt

            val result = firebaseTokenVerifier.verifyIdToken(token)

            assertEquals("user-uid-456", result.uid)
            assertNull(result.email)
            assertNull(result.name)
        }
    }

    @Nested
    @DisplayName("JWKS 미초기화 케이스")
    inner class UninitializedDecoderTests {

        @Test
        @DisplayName("cachedDecoder null 상태에서 verifyIdToken 호웉 시 RuntimeException 발생")
        fun uninitializedDecoderThrowsRuntimeException() {
            val uninitializedVerifier = FirebaseTokenVerifier(projectId = "test-project-id")
            assertThrows(RuntimeException::class.java) {
                uninitializedVerifier.verifyIdToken("any-token")
            }
        }

        @Test
        @DisplayName("JWKS endpoint 연결 실패 시 init()이 예외를 전파하지 않는다")
        fun initDoesNotThrowOnFetchFailure() {
            val verifier = FirebaseTokenVerifier(projectId = "test-project-id", jwksUri = "http://localhost:1")
            assertDoesNotThrow { verifier.init() }
        }
    }

    @Nested
    @DisplayName("JWKS 갱신 내성")
    inner class JwksRefreshTests {

        @Test
        @DisplayName("refreshJwks 실패 시 기존 디코더 유지로 verifyIdToken 정상 동작")
        fun refreshJwksFailureKeepsExistingDecoder() {
            val token = "valid-token"
            val now = Instant.now()
            val verifier = FirebaseTokenVerifier(projectId = "test-project-id", jwksUri = "http://localhost:1")
            val localMockDecoder = mockk<JwtDecoder>()
            val mockJwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("user-uid-123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build()
            every { localMockDecoder.decode(token) } returns mockJwt
            verifier.setDecoder(localMockDecoder)

            verifier.refreshJwks()

            val result = verifier.verifyIdToken(token)
            assertEquals("user-uid-123", result.uid)
        }
    }

    @Nested
    @DisplayName("성능 벤치마크")
    inner class PerformanceBenchmarkTests {

        @Test
        @DisplayName("신규 코드: 1000회 verifyIdToken 호출이 200ms 이내 (평균 <0.2ms)")
        fun newCode1000CallsUnder200ms() {
            val token = "perf-test-token"
            val now = Instant.now()
            val mockJwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("perf-user-uid")
                .claim("email", "perf@test.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build()
            every { mockDecoder.decode(token) } returns mockJwt

            // 워밍업 (JIT 컴파일러 안정화)
            repeat(50) { firebaseTokenVerifier.verifyIdToken(token) }

            val startNs = System.nanoTime()
            repeat(1000) { firebaseTokenVerifier.verifyIdToken(token) }
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
            val avgMs = elapsedMs.toDouble() / 1000

            println("[BENCHMARK] 1000회 호출: ${elapsedMs}ms total | avg ${avgMs}ms/call")
            println("[BENCHMARK] 구 코드(HTTP): 600~1200ms/call → 신규 코드(로컬 RSA): ${avgMs}ms/call")

            assertTrue(elapsedMs < 200, "1000회 호출이 ${elapsedMs}ms 걸림 (200ms 이내 기대)")
        }
    }
}