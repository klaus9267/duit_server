package duit.server.domain.auth.service

import duit.server.application.security.JwtTokenProvider
import duit.server.domain.auth.dto.FirebaseTokenClaims
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import duit.server.domain.user.service.UserService
import duit.server.infrastructure.external.firebase.FirebaseTokenVerifier
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("AuthService 단위 테스트")
class AuthServiceUnitTest {

    @Nested
    @DisplayName("socialLogin")
    inner class SocialLoginTests {

        @Nested
        @DisplayName("토큰 검증")
        inner class TokenValidationTests {

            private lateinit var firebaseTokenVerifier: FirebaseTokenVerifier
            private lateinit var jwtTokenProvider: JwtTokenProvider
            private lateinit var userRepository: UserRepository
            private lateinit var userService: UserService
            private lateinit var authService: AuthService

            @BeforeEach
            fun setUp() {
                firebaseTokenVerifier = mockk(relaxed = true)
                jwtTokenProvider = mockk()
                userRepository = mockk()
                userService = mockk()
                authService = AuthService(firebaseTokenVerifier, jwtTokenProvider, userRepository, userService)
            }

            @Test
            @DisplayName("빈 문자열 - IllegalArgumentException 발생")
            fun emptyTokenThrowsException() {
                val exception = assertThrows<IllegalArgumentException> {
                    authService.socialLogin("")
                }
                assertTrue(exception.message!!.contains("ID 토큰이 비어있습니다"))
            }

            @Test
            @DisplayName("null 문자열 - IllegalArgumentException 발생")
            fun nullStringThrowsException() {
                val exception = assertThrows<IllegalArgumentException> {
                    authService.socialLogin("null")
                }
                assertTrue(exception.message!!.contains("ID 토큰이 비어있습니다"))
            }

            @Test
            @DisplayName("공백만 있는 문자열 - IllegalArgumentException 발생")
            fun blankTokenThrowsException() {
                val exception = assertThrows<IllegalArgumentException> {
                    authService.socialLogin("   ")
                }
                assertTrue(exception.message!!.contains("ID 토큰이 비어있습니다"))
            }

            @Test
            @DisplayName("FirebaseTokenVerifier가 IllegalArgumentException throw - 그대로 전파")
            fun firebaseVerifierThrowsIllegalArgumentException() {
                val invalidToken = "invalid-token"
                every { firebaseTokenVerifier.verifyIdToken(invalidToken) } throws IllegalArgumentException("Invalid token format")

                val exception = assertThrows<IllegalArgumentException> {
                    authService.socialLogin(invalidToken)
                }
                assertTrue(exception.message!!.contains("Invalid token format"))
            }
        }

        @Nested
        @DisplayName("기존 유저 로그인")
        inner class ExistingUserTests {

            private lateinit var firebaseTokenVerifier: FirebaseTokenVerifier
            private lateinit var jwtTokenProvider: JwtTokenProvider
            private lateinit var userRepository: UserRepository
            private lateinit var userService: UserService
            private lateinit var authService: AuthService

            @BeforeEach
            fun setUp() {
                firebaseTokenVerifier = mockk(relaxed = true)
                jwtTokenProvider = mockk()
                userRepository = mockk()
                userService = mockk()
                authService = AuthService(firebaseTokenVerifier, jwtTokenProvider, userRepository, userService)
            }

            @Test
            @DisplayName("기존 유저 존재 - isNewUser=false 반환")
            fun existingUserReturnsIsNewUserFalse() {
                val idToken = "valid-token"
                val providerId = "existing-uid"
                val existingUser = User(
                    id = 1L,
                    nickname = "기존유저",
                    providerId = providerId,
                    providerType = ProviderType.GOOGLE
                )

                val tokenClaims = FirebaseTokenClaims(
                    uid = providerId,
                    email = "test@example.com",
                    name = "기존유저",
                    claims = mapOf("firebase" to mapOf("sign_in_provider" to "google.com"))
                )

                every { firebaseTokenVerifier.verifyIdToken(idToken) } returns tokenClaims
                every { userRepository.findByProviderId(providerId) } returns existingUser
                every { jwtTokenProvider.createAccessToken(existingUser.id!!) } returns "jwt-access-token"

                val result = authService.socialLogin(idToken)

                assertFalse(result.isNewUser, "기존 유저이므로 isNewUser는 false여야 합니다")
                assertEquals("jwt-access-token", result.accessToken)
                assertEquals(existingUser.id, result.user.id)
                assertEquals(existingUser.nickname, result.user.nickname)
                verify(exactly = 1) { userRepository.findByProviderId(providerId) }
                verify(exactly = 0) { userService.createUser(any(), any(), any(), any()) }
            }
        }

        @Nested
        @DisplayName("신규 유저 로그인")
        inner class NewUserTests {

            private lateinit var firebaseTokenVerifier: FirebaseTokenVerifier
            private lateinit var jwtTokenProvider: JwtTokenProvider
            private lateinit var userRepository: UserRepository
            private lateinit var userService: UserService
            private lateinit var authService: AuthService

            @BeforeEach
            fun setUp() {
                firebaseTokenVerifier = mockk(relaxed = true)
                jwtTokenProvider = mockk()
                userRepository = mockk()
                userService = mockk()
                authService = AuthService(firebaseTokenVerifier, jwtTokenProvider, userRepository, userService)
            }

            @Test
            @DisplayName("신규 유저 - isNewUser=true 반환 및 userService.createUser 호출")
            fun newUserReturnsIsNewUserTrue() {
                val idToken = "valid-token"
                val providerId = "new-uid"
                val newUser = User(
                    id = 2L,
                    nickname = "신규유저",
                    providerId = providerId,
                    providerType = ProviderType.GOOGLE
                )

                val tokenClaims = FirebaseTokenClaims(
                    uid = providerId,
                    email = "new@example.com",
                    name = "신규유저",
                    claims = mapOf("firebase" to mapOf("sign_in_provider" to "google.com"))
                )

                every { firebaseTokenVerifier.verifyIdToken(idToken) } returns tokenClaims
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.GOOGLE, providerId, "new@example.com", "신규유저") } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-new-user-token"

                val result = authService.socialLogin(idToken)

                assertTrue(result.isNewUser, "신규 유저이므로 isNewUser는 true여야 합니다")
                assertEquals("jwt-new-user-token", result.accessToken)
                assertEquals(newUser.id, result.user.id)
                assertEquals(newUser.nickname, result.user.nickname)
                verify(exactly = 1) { userRepository.findByProviderId(providerId) }
                verify(exactly = 1) { userService.createUser(ProviderType.GOOGLE, providerId, "new@example.com", "신규유저") }
            }
        }

        @Nested
        @DisplayName("프로바이더 타입 결정")
        inner class ProviderTypeTests {

            private lateinit var firebaseTokenVerifier: FirebaseTokenVerifier
            private lateinit var jwtTokenProvider: JwtTokenProvider
            private lateinit var userRepository: UserRepository
            private lateinit var userService: UserService
            private lateinit var authService: AuthService

            @BeforeEach
            fun setUp() {
                firebaseTokenVerifier = mockk(relaxed = true)
                jwtTokenProvider = mockk()
                userRepository = mockk()
                userService = mockk()
                authService = AuthService(firebaseTokenVerifier, jwtTokenProvider, userRepository, userService)
            }

            @Test
            @DisplayName("sign_in_provider가 google.com - GOOGLE 반환")
            fun googleProviderReturnsGoogle() {
                val idToken = "valid-token"
                val providerId = "google-uid"
                val newUser = User(
                    id = 3L,
                    nickname = "구글유저",
                    providerId = providerId,
                    providerType = ProviderType.GOOGLE
                )

                val tokenClaims = FirebaseTokenClaims(
                    uid = providerId,
                    email = "google@example.com",
                    name = "구글유저",
                    claims = mapOf("firebase" to mapOf("sign_in_provider" to "google.com"))
                )

                every { firebaseTokenVerifier.verifyIdToken(idToken) } returns tokenClaims
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.GOOGLE, providerId, "google@example.com", "구글유저") } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-token"

                authService.socialLogin(idToken)

                verify(exactly = 1) { userService.createUser(ProviderType.GOOGLE, providerId, "google@example.com", "구글유저") }
            }

            @Test
            @DisplayName("sign_in_provider가 apple.com - APPLE 반환")
            fun appleProviderReturnsApple() {
                val idToken = "valid-token"
                val providerId = "apple-uid"
                val newUser = User(
                    id = 4L,
                    nickname = "애플유저",
                    providerId = providerId,
                    providerType = ProviderType.APPLE
                )

                val tokenClaims = FirebaseTokenClaims(
                    uid = providerId,
                    email = "apple@example.com",
                    name = "애플유저",
                    claims = mapOf("firebase" to mapOf("sign_in_provider" to "apple.com"))
                )

                every { firebaseTokenVerifier.verifyIdToken(idToken) } returns tokenClaims
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.APPLE, providerId, "apple@example.com", "애플유저") } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-token"

                authService.socialLogin(idToken)

                verify(exactly = 1) { userService.createUser(ProviderType.APPLE, providerId, "apple@example.com", "애플유저") }
            }

            @Test
            @DisplayName("sign_in_provider가 oidc.kakao - KAKAO 반환")
            fun kakaoProviderReturnsKakao() {
                val idToken = "valid-token"
                val providerId = "kakao-uid"
                val newUser = User(
                    id = 5L,
                    nickname = "카카오유저",
                    providerId = providerId,
                    providerType = ProviderType.KAKAO
                )

                val tokenClaims = FirebaseTokenClaims(
                    uid = providerId,
                    email = "kakao@example.com",
                    name = "카카오유저",
                    claims = mapOf("firebase" to mapOf("sign_in_provider" to "oidc.kakao"))
                )

                every { firebaseTokenVerifier.verifyIdToken(idToken) } returns tokenClaims
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.KAKAO, providerId, "kakao@example.com", "카카오유저") } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-token"

                authService.socialLogin(idToken)

                verify(exactly = 1) { userService.createUser(ProviderType.KAKAO, providerId, "kakao@example.com", "카카오유저") }
            }

            @Test
            @DisplayName("sign_in_provider가 oidc.kakao_rest - KAKAO 반환")
            fun kakaoRestProviderReturnsKakao() {
                val idToken = "valid-token"
                val providerId = "kakao-rest-uid"
                val newUser = User(
                    id = 6L,
                    nickname = "카카오REST유저",
                    providerId = providerId,
                    providerType = ProviderType.KAKAO
                )

                val tokenClaims = FirebaseTokenClaims(
                    uid = providerId,
                    email = "kakao-rest@example.com",
                    name = "카카오REST유저",
                    claims = mapOf("firebase" to mapOf("sign_in_provider" to "oidc.kakao_rest"))
                )

                every { firebaseTokenVerifier.verifyIdToken(idToken) } returns tokenClaims
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.KAKAO, providerId, "kakao-rest@example.com", "카카오REST유저") } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-token"

                authService.socialLogin(idToken)

                verify(exactly = 1) { userService.createUser(ProviderType.KAKAO, providerId, "kakao-rest@example.com", "카카오REST유저") }
            }

            @Test
            @DisplayName("알 수 없는 sign_in_provider - RuntimeException 발생")
            fun unknownProviderThrowsRuntimeException() {
                val idToken = "valid-token"
                val providerId = "unknown-uid"

                val tokenClaims = FirebaseTokenClaims(
                    uid = providerId,
                    email = null,
                    name = null,
                    claims = mapOf("firebase" to mapOf("sign_in_provider" to "unknown.provider"))
                )

                every { firebaseTokenVerifier.verifyIdToken(idToken) } returns tokenClaims
                every { userRepository.findByProviderId(providerId) } returns null

                val exception = assertThrows<RuntimeException> {
                    authService.socialLogin(idToken)
                }
                assertTrue(exception.message!!.contains("잘못된 소셜로그인 사용 정보 입니다"))
            }

            @Test
            @DisplayName("firebase claims가 없는 경우 - RuntimeException 발생")
            fun missingFirebaseClaimsThrowsRuntimeException() {
                val idToken = "valid-token"
                val providerId = "no-claims-uid"

                val tokenClaims = FirebaseTokenClaims(
                    uid = providerId,
                    email = null,
                    name = null,
                    claims = mapOf<String, Any>()
                )

                every { firebaseTokenVerifier.verifyIdToken(idToken) } returns tokenClaims
                every { userRepository.findByProviderId(providerId) } returns null

                val exception = assertThrows<RuntimeException> {
                    authService.socialLogin(idToken)
                }
                assertTrue(exception.message!!.contains("잘못된 소셜로그인 사용 정보 입니다"))
            }

            @Test
            @DisplayName("firebase claims의 sign_in_provider가 null인 경우 - RuntimeException 발생")
            fun nullSignInProviderThrowsRuntimeException() {
                val idToken = "valid-token"
                val providerId = "null-provider-uid"

                val tokenClaims = FirebaseTokenClaims(
                    uid = providerId,
                    email = null,
                    name = null,
                    claims = mapOf("firebase" to mapOf<String, Any?>())
                )

                every { firebaseTokenVerifier.verifyIdToken(idToken) } returns tokenClaims
                every { userRepository.findByProviderId(providerId) } returns null

                val exception = assertThrows<RuntimeException> {
                    authService.socialLogin(idToken)
                }
                assertTrue(exception.message!!.contains("잘못된 소셜로그인 사용 정보 입니다"))
            }
        }
    }
}
