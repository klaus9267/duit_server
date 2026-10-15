package duit.server.domain.auth.controller

import com.ninjasquad.springmockk.MockkBean
import duit.server.domain.auth.dto.FirebaseTokenClaims
import duit.server.domain.user.entity.ProviderType
import duit.server.infrastructure.external.firebase.FirebaseTokenVerifier
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import io.mockk.clearMocks
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Auth API 통합 테스트")
class AuthControllerIntegrationTest : IntegrationTestSupport() {

    @MockkBean
    private lateinit var firebaseTokenVerifier: FirebaseTokenVerifier

    @BeforeEach
    fun resetFirebaseTokenVerifierMock() {
        clearMocks(firebaseTokenVerifier, answers = true, recordedCalls = true)
    }

    private fun stubFirebaseToken(uid: String, provider: String, name: String? = null, email: String? = null) {
        val claims = FirebaseTokenClaims(
            uid = uid,
            email = email,
            name = name,
            claims = mapOf("firebase" to mapOf("sign_in_provider" to provider))
        )
        every { firebaseTokenVerifier.verifyIdToken(any()) } returns claims
    }

    @Nested
    @DisplayName("POST /api/v1/auth/social - 소셜 로그인")
    inner class SocialLoginTests {

        @Nested
        @DisplayName("성공 - 기존 유저 로그인")
        inner class ExistingUserSuccess {

            @Test
            @DisplayName("기존 유저가 소셜 로그인하면 isNewUser=false와 JWT 토큰을 반환한다")
            fun existingUserLogin() {
                val user = TestFixtures.user(
                    nickname = "기존유저",
                    providerType = ProviderType.GOOGLE,
                    providerId = "existing-uid",
                    email = "existing@example.com",
                )
                entityManager.persist(user)
                entityManager.flush()
                entityManager.clear()

                stubFirebaseToken(uid = "existing-uid", provider = "google.com")

                mockMvc.perform(
                    post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("valid-firebase-token"))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").isString)
                    .andExpect(jsonPath("$.isNewUser").value(false))
                    .andExpect(jsonPath("$.user.id").value(user.id!!.toInt()))
                    .andExpect(jsonPath("$.user.nickname").value("기존유저"))
                    .andExpect(jsonPath("$.user.providerId").value("existing-uid"))
            }
        }

        @Nested
        @DisplayName("성공 - 신규 유저 회원가입")
        inner class NewUserSuccess {

            @Test
            @DisplayName("Google 신규 유저가 소셜 로그인하면 isNewUser=true와 JWT 토큰을 반환한다")
            fun newUserGoogleSignup() {
                stubFirebaseToken(uid = "new-uid", provider = "google.com", name = "새유저", email = "new@example.com")

                mockMvc.perform(
                    post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("new-user-token"))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").isString)
                    .andExpect(jsonPath("$.isNewUser").value(true))
                    .andExpect(jsonPath("$.user.nickname").value("새유저"))
                    .andExpect(jsonPath("$.user.providerId").value("new-uid"))
            }

            @Test
            @DisplayName("Apple 신규 유저도 정상적으로 회원가입된다")
            fun newUserAppleSignup() {
                stubFirebaseToken(uid = "apple-uid", provider = "apple.com", name = "애플유저", email = "apple@example.com")

                mockMvc.perform(
                    post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("apple-token"))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").isString)
                    .andExpect(jsonPath("$.isNewUser").value(true))
                    .andExpect(jsonPath("$.user.providerId").value("apple-uid"))
            }

            @Test
            @DisplayName("Kakao 신규 유저도 정상적으로 회원가입된다")
            fun newUserKakaoSignup() {
                stubFirebaseToken(uid = "kakao-uid", provider = "oidc.kakao", name = "카카오유저", email = "kakao@example.com")

                mockMvc.perform(
                    post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("kakao-token"))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").isString)
                    .andExpect(jsonPath("$.isNewUser").value(true))
                    .andExpect(jsonPath("$.user.providerId").value("kakao-uid"))
            }
        }

        @Nested
        @DisplayName("성공 - 약관 동의 기록")
        inner class TermsAgreementSuccess {

            private fun termsOf(providerId: String) = entityManager
                .createQuery(
                    "SELECT u.termsVersion, u.termsAgreedAt FROM User u WHERE u.providerId = :providerId",
                    Array<Any?>::class.java,
                )
                .setParameter("providerId", providerId)
                .singleResult

            @Test
            @DisplayName("JSON 본문으로 약관 버전을 보내면 버전과 동의 시각을 저장한다")
            fun recordsAgreedTermsVersion() {
                stubFirebaseToken(uid = "terms-uid", provider = "google.com", name = "약관유저", email = "terms@example.com")

                mockMvc.perform(
                    post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf("idToken" to "terms-token", "agreedTermsVersion" to "2026-10-15")
                            )
                        )
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.isNewUser").value(true))
                    .andExpect(jsonPath("$.user.providerId").value("terms-uid"))

                entityManager.flush()
                entityManager.clear()
                val (version, agreedAt) = termsOf("terms-uid")
                assertEquals("2026-10-15", version)
                assertNotNull(agreedAt)
            }

            @Test
            @DisplayName("약관 버전 없이 로그인하면 기존 동의 기록을 유지한다")
            fun keepsTermsWhenVersionMissing() {
                val user = TestFixtures.user(providerId = "terms-keep-uid").apply { agreeToTerms("2026-10-15") }
                entityManager.persist(user)
                entityManager.flush()
                entityManager.clear()
                stubFirebaseToken(uid = "terms-keep-uid", provider = "google.com")

                mockMvc.perform(
                    post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mapOf("idToken" to "terms-token")))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.isNewUser").value(false))

                entityManager.flush()
                entityManager.clear()
                assertEquals("2026-10-15", termsOf("terms-keep-uid")[0])
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("약관 버전 형식이 잘못되면 400을 반환한다")
            fun invalidTermsVersion() {
                stubFirebaseToken(uid = "invalid-terms-uid", provider = "google.com")

                mockMvc.perform(
                    post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf("idToken" to "terms-token", "agreedTermsVersion" to "latest")
                            )
                        )
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("유효하지 않은 Firebase 토큰이면 에러를 반환한다")
            fun invalidFirebaseToken() {
                every { firebaseTokenVerifier.verifyIdToken(any()) } throws
                    IllegalArgumentException("Invalid token format")

                mockMvc.perform(
                    post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("invalid-token"))
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("알 수 없는 소셜 프로바이더이면 에러를 반환한다")
            fun unknownProvider() {
                stubFirebaseToken(uid = "unknown-uid", provider = "unknown.provider")

                mockMvc.perform(
                    post("/api/v1/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("unknown-provider-token"))
                )
                    .andDo(print())
                    .andExpect(status().isInternalServerError)
            }
        }
    }
}
