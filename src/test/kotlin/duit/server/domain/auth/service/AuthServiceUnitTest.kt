package duit.server.domain.auth.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import duit.server.application.security.JwtTokenProvider
import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import duit.server.domain.user.service.UserService
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

            private lateinit var firebaseAuth: FirebaseAuth
            private lateinit var jwtTokenProvider: JwtTokenProvider
            private lateinit var userRepository: UserRepository
            private lateinit var userService: UserService
            private lateinit var authService: AuthService

            @BeforeEach
            fun setUp() {
                firebaseAuth = mockk(relaxed = true)
                jwtTokenProvider = mockk()
                userRepository = mockk()
                userService = mockk()
                authService = AuthService(firebaseAuth, jwtTokenProvider, userRepository, userService)
            }

            @Test
            @DisplayName("빈 문자열 - IllegalArgumentException 발생")
            fun emptyTokenThrowsException() {
                // when & then
                val exception = assertThrows<IllegalArgumentException> {
                    authService.socialLogin("")
                }
                assertTrue(exception.message!!.contains("ID 토큰이 비어있습니다"))
            }

            @Test
            @DisplayName("null 문자열 - IllegalArgumentException 발생")
            fun nullStringThrowsException() {
                // when & then
                val exception = assertThrows<IllegalArgumentException> {
                    authService.socialLogin("null")
                }
                assertTrue(exception.message!!.contains("ID 토큰이 비어있습니다"))
            }

            @Test
            @DisplayName("공백만 있는 문자열 - IllegalArgumentException 발생")
            fun blankTokenThrowsException() {
                // when & then
                val exception = assertThrows<IllegalArgumentException> {
                    authService.socialLogin("   ")
                }
                assertTrue(exception.message!!.contains("ID 토큰이 비어있습니다"))
            }

            @Test
            @DisplayName("Firebase verifyIdToken이 IllegalArgumentException throw - 재래핑하여 IllegalArgumentException 발생")
            fun firebaseVerifyThrowsIllegalArgumentException() {
                // given
                val invalidToken = "invalid-token"
                every { firebaseAuth.verifyIdToken(invalidToken) } throws IllegalArgumentException("Invalid token format")

                // when & then
                val exception = assertThrows<IllegalArgumentException> {
                    authService.socialLogin(invalidToken)
                }
                assertTrue(exception.message!!.contains("유효하지 않은 토큰 형식입니다"))
                assertTrue(exception.message!!.contains(invalidToken))
            }

            @Test
            @DisplayName("Firebase verifyIdToken이 FirebaseAuthException throw - IllegalArgumentException으로 변환")
            fun firebaseVerifyThrowsFirebaseAuthException() {
                // given
                val invalidToken = "expired-token"
                val firebaseException = mockk<FirebaseAuthException>()
                every { firebaseException.message } returns "Token expired"
                every { firebaseAuth.verifyIdToken(invalidToken) } throws firebaseException

                // when & then
                val exception = assertThrows<IllegalArgumentException> {
                    authService.socialLogin(invalidToken)
                }
                assertTrue(exception.message!!.contains("Firebase 토큰 검증 실패"))
                assertTrue(exception.message!!.contains(invalidToken))
                assertTrue(exception.message!!.contains("Token expired"))
            }
        }

        @Nested
        @DisplayName("기존 유저 로그인")
        inner class ExistingUserTests {

            private lateinit var firebaseAuth: FirebaseAuth
            private lateinit var jwtTokenProvider: JwtTokenProvider
            private lateinit var userRepository: UserRepository
            private lateinit var userService: UserService
            private lateinit var authService: AuthService

            @BeforeEach
            fun setUp() {
                firebaseAuth = mockk(relaxed = true)
                jwtTokenProvider = mockk()
                userRepository = mockk()
                userService = mockk()
                authService = AuthService(firebaseAuth, jwtTokenProvider, userRepository, userService)
            }

            @Test
            @DisplayName("기존 유저 존재 - isNewUser=false 반환")
            fun existingUserReturnsIsNewUserFalse() {
                // given
                val idToken = "valid-token"
                val providerId = "existing-uid"
                val existingUser = User(
                    id = 1L,
                    nickname = "기존유저",
                    providerId = providerId,
                    providerType = ProviderType.GOOGLE
                )

                val mockToken = mockk<FirebaseToken>()
                every { mockToken.uid } returns providerId
                every { mockToken.claims } returns mapOf(
                    "firebase" to mapOf("sign_in_provider" to "google.com")
                )

                every { firebaseAuth.verifyIdToken(idToken) } returns mockToken
                every { userRepository.findByProviderId(providerId) } returns existingUser
                every { jwtTokenProvider.createAccessToken(existingUser.id!!) } returns "jwt-access-token"

                // when
                val result = authService.socialLogin(idToken)

                // then
                assertFalse(result.isNewUser, "기존 유저이므로 isNewUser는 false여야 합니다")
                assertEquals("jwt-access-token", result.accessToken)
                assertEquals(existingUser.id, result.user.id)
                assertEquals(existingUser.nickname, result.user.nickname)
                verify(exactly = 1) { userRepository.findByProviderId(providerId) }
                verify(exactly = 0) { userService.createUser(any(), any()) }
            }
        }

        @Nested
        @DisplayName("신규 유저 로그인")
        inner class NewUserTests {

            private lateinit var firebaseAuth: FirebaseAuth
            private lateinit var jwtTokenProvider: JwtTokenProvider
            private lateinit var userRepository: UserRepository
            private lateinit var userService: UserService
            private lateinit var authService: AuthService

            @BeforeEach
            fun setUp() {
                firebaseAuth = mockk(relaxed = true)
                jwtTokenProvider = mockk()
                userRepository = mockk()
                userService = mockk()
                authService = AuthService(firebaseAuth, jwtTokenProvider, userRepository, userService)
            }

            @Test
            @DisplayName("신규 유저 - isNewUser=true 반환 및 userService.createUser 호출")
            fun newUserReturnsIsNewUserTrue() {
                // given
                val idToken = "valid-token"
                val providerId = "new-uid"
                val newUser = User(
                    id = 2L,
                    nickname = "신규유저",
                    providerId = providerId,
                    providerType = ProviderType.GOOGLE
                )

                val mockToken = mockk<FirebaseToken>()
                every { mockToken.uid } returns providerId
                every { mockToken.claims } returns mapOf(
                    "firebase" to mapOf("sign_in_provider" to "google.com")
                )

                every { firebaseAuth.verifyIdToken(idToken) } returns mockToken
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.GOOGLE, mockToken) } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-new-user-token"

                // when
                val result = authService.socialLogin(idToken)

                // then
                assertTrue(result.isNewUser, "신규 유저이므로 isNewUser는 true여야 합니다")
                assertEquals("jwt-new-user-token", result.accessToken)
                assertEquals(newUser.id, result.user.id)
                assertEquals(newUser.nickname, result.user.nickname)
                verify(exactly = 1) { userRepository.findByProviderId(providerId) }
                verify(exactly = 1) { userService.createUser(ProviderType.GOOGLE, mockToken) }
            }
        }

        @Nested
        @DisplayName("프로바이더 타입 결정")
        inner class ProviderTypeTests {

            private lateinit var firebaseAuth: FirebaseAuth
            private lateinit var jwtTokenProvider: JwtTokenProvider
            private lateinit var userRepository: UserRepository
            private lateinit var userService: UserService
            private lateinit var authService: AuthService

            @BeforeEach
            fun setUp() {
                firebaseAuth = mockk(relaxed = true)
                jwtTokenProvider = mockk()
                userRepository = mockk()
                userService = mockk()
                authService = AuthService(firebaseAuth, jwtTokenProvider, userRepository, userService)
            }

            @Test
            @DisplayName("sign_in_provider가 google.com - GOOGLE 반환")
            fun googleProviderReturnsGoogle() {
                // given
                val idToken = "valid-token"
                val providerId = "google-uid"
                val newUser = User(
                    id = 3L,
                    nickname = "구글유저",
                    providerId = providerId,
                    providerType = ProviderType.GOOGLE
                )

                val mockToken = mockk<FirebaseToken>()
                every { mockToken.uid } returns providerId
                every { mockToken.claims } returns mapOf(
                    "firebase" to mapOf("sign_in_provider" to "google.com")
                )

                every { firebaseAuth.verifyIdToken(idToken) } returns mockToken
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.GOOGLE, mockToken) } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-token"

                // when
                authService.socialLogin(idToken)

                // then
                verify(exactly = 1) { userService.createUser(ProviderType.GOOGLE, mockToken) }
            }

            @Test
            @DisplayName("sign_in_provider가 apple.com - APPLE 반환")
            fun appleProviderReturnsApple() {
                // given
                val idToken = "valid-token"
                val providerId = "apple-uid"
                val newUser = User(
                    id = 4L,
                    nickname = "애플유저",
                    providerId = providerId,
                    providerType = ProviderType.APPLE
                )

                val mockToken = mockk<FirebaseToken>()
                every { mockToken.uid } returns providerId
                every { mockToken.claims } returns mapOf(
                    "firebase" to mapOf("sign_in_provider" to "apple.com")
                )

                every { firebaseAuth.verifyIdToken(idToken) } returns mockToken
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.APPLE, mockToken) } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-token"

                // when
                authService.socialLogin(idToken)

                // then
                verify(exactly = 1) { userService.createUser(ProviderType.APPLE, mockToken) }
            }

            @Test
            @DisplayName("sign_in_provider가 oidc.kakao - KAKAO 반환")
            fun kakaoProviderReturnsKakao() {
                // given
                val idToken = "valid-token"
                val providerId = "kakao-uid"
                val newUser = User(
                    id = 5L,
                    nickname = "카카오유저",
                    providerId = providerId,
                    providerType = ProviderType.KAKAO
                )

                val mockToken = mockk<FirebaseToken>()
                every { mockToken.uid } returns providerId
                every { mockToken.claims } returns mapOf(
                    "firebase" to mapOf("sign_in_provider" to "oidc.kakao")
                )

                every { firebaseAuth.verifyIdToken(idToken) } returns mockToken
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.KAKAO, mockToken) } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-token"

                // when
                authService.socialLogin(idToken)

                // then
                verify(exactly = 1) { userService.createUser(ProviderType.KAKAO, mockToken) }
            }

            @Test
            @DisplayName("sign_in_provider가 oidc.kakao_rest - KAKAO 반환")
            fun kakaoRestProviderReturnsKakao() {
                // given
                val idToken = "valid-token"
                val providerId = "kakao-rest-uid"
                val newUser = User(
                    id = 6L,
                    nickname = "카카오REST유저",
                    providerId = providerId,
                    providerType = ProviderType.KAKAO
                )

                val mockToken = mockk<FirebaseToken>()
                every { mockToken.uid } returns providerId
                every { mockToken.claims } returns mapOf(
                    "firebase" to mapOf("sign_in_provider" to "oidc.kakao_rest")
                )

                every { firebaseAuth.verifyIdToken(idToken) } returns mockToken
                every { userRepository.findByProviderId(providerId) } returns null
                every { userService.createUser(ProviderType.KAKAO, mockToken) } returns newUser
                every { jwtTokenProvider.createAccessToken(newUser.id!!) } returns "jwt-token"

                // when
                authService.socialLogin(idToken)

                // then
                verify(exactly = 1) { userService.createUser(ProviderType.KAKAO, mockToken) }
            }

            @Test
            @DisplayName("알 수 없는 sign_in_provider - RuntimeException 발생")
            fun unknownProviderThrowsRuntimeException() {
                // given
                val idToken = "valid-token"
                val providerId = "unknown-uid"

                val mockToken = mockk<FirebaseToken>()
                every { mockToken.uid } returns providerId
                every { mockToken.claims } returns mapOf(
                    "firebase" to mapOf("sign_in_provider" to "unknown.provider")
                )

                every { firebaseAuth.verifyIdToken(idToken) } returns mockToken
                every { userRepository.findByProviderId(providerId) } returns null

                // when & then
                val exception = assertThrows<RuntimeException> {
                    authService.socialLogin(idToken)
                }
                assertTrue(exception.message!!.contains("잘못된 소셜로그인 사용 정보 입니다"))
            }

            @Test
            @DisplayName("firebase claims가 없는 경우 - RuntimeException 발생")
            fun missingFirebaseClaimsThrowsRuntimeException() {
                // given
                val idToken = "valid-token"
                val providerId = "no-claims-uid"

                val mockToken = mockk<FirebaseToken>()
                every { mockToken.uid } returns providerId
                every { mockToken.claims } returns mapOf<String, Any>()

                every { firebaseAuth.verifyIdToken(idToken) } returns mockToken
                every { userRepository.findByProviderId(providerId) } returns null

                // when & then
                val exception = assertThrows<RuntimeException> {
                    authService.socialLogin(idToken)
                }
                assertTrue(exception.message!!.contains("잘못된 소셜로그인 사용 정보 입니다"))
            }

            @Test
            @DisplayName("firebase claims의 sign_in_provider가 null인 경우 - RuntimeException 발생")
            fun nullSignInProviderThrowsRuntimeException() {
                // given
                val idToken = "valid-token"
                val providerId = "null-provider-uid"

                val mockToken = mockk<FirebaseToken>()
                every { mockToken.uid } returns providerId
                every { mockToken.claims } returns mapOf(
                    "firebase" to mapOf<String, Any?>()
                )

                every { firebaseAuth.verifyIdToken(idToken) } returns mockToken
                every { userRepository.findByProviderId(providerId) } returns null

                // when & then
                val exception = assertThrows<RuntimeException> {
                    authService.socialLogin(idToken)
                }
                assertTrue(exception.message!!.contains("잘못된 소셜로그인 사용 정보 입니다"))
            }
        }
    }
}
