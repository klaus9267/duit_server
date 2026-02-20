package duit.server.domain.auth.controller

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.ninjasquad.springmockk.MockkBean
import duit.server.domain.user.entity.ProviderType
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
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

    @MockkBean(relaxed = true)
    private lateinit var firebaseAuth: FirebaseAuth

    @BeforeEach
    fun resetFirebaseAuthMock() {
        clearMocks(firebaseAuth, answers = true, recordedCalls = true)
    }

    private fun stubFirebaseToken(uid: String, provider: String, name: String? = null, email: String? = null): FirebaseToken {
        val mockToken = mockk<FirebaseToken>()
        every { mockToken.uid } returns uid
        every { mockToken.name } returns name
        every { mockToken.email } returns email
        every { mockToken.claims } returns mapOf(
            "firebase" to mapOf("sign_in_provider" to provider),
        )
        every { firebaseAuth.verifyIdToken(any<String>()) } returns mockToken
        every { firebaseAuth.verifyIdToken(any<String>(), any()) } returns mockToken
        return mockToken
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
                // given
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

                // when & then
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
                // given
                stubFirebaseToken(uid = "new-uid", provider = "google.com", name = "새유저", email = "new@example.com")

                // when & then
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
                // given
                stubFirebaseToken(uid = "apple-uid", provider = "apple.com", name = "애플유저", email = "apple@example.com")

                // when & then
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
                // given
                stubFirebaseToken(uid = "kakao-uid", provider = "oidc.kakao", name = "카카오유저", email = "kakao@example.com")

                // when & then
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
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("유효하지 않은 Firebase 토큰이면 에러를 반환한다")
            fun invalidFirebaseToken() {
                // given
                every { firebaseAuth.verifyIdToken(any<String>()) } throws
                    IllegalArgumentException("Invalid token format")
                every { firebaseAuth.verifyIdToken(any<String>(), any()) } throws
                    IllegalArgumentException("Invalid token format")

                // when & then
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
                // given
                stubFirebaseToken(uid = "unknown-uid", provider = "unknown.provider")

                // when & then
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
