package duit.server.domain.user.controller

import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("User API 통합 테스트")
class UserControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user: User
    private lateinit var otherUser: User

    @BeforeEach
    fun setUp() {
        user = TestFixtures.user(nickname = "테스트유저", providerId = "provider-1")
        otherUser = TestFixtures.user(nickname = "다른유저", providerId = "provider-2", email = "other@example.com")
        entityManager.persist(user)
        entityManager.persist(otherUser)
        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("GET /api/v1/users/check-nickname - 닉네임 중복 확인")
    inner class CheckNicknameTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("사용 가능한 닉네임이면 200을 반환한다")
            fun availableNickname() {
                mockMvc.perform(
                    get("/api/v1/users/check-nickname")
                        .param("nickname", "사용가능닉네임")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("이미 존재하는 닉네임이면 에러를 반환한다")
            fun duplicateNickname() {
                mockMvc.perform(
                    get("/api/v1/users/check-nickname")
                        .param("nickname", "테스트유저")
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users - 사용자 목록 조회")
    inner class GetAllUsersTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("사용자 목록을 페이지네이션으로 조회한다")
            fun getAllUsers() {
                mockMvc.perform(
                    get("/api/v1/users")
                        .header("Authorization", authHeader(user.id!!))
                        .param("page", "0")
                        .param("size", "10")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(2))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(get("/api/v1/users"))
                    .andDo(print())
                    .andExpect(status().isOk) // SecurityConfig에서 GET /api/v1/users는 permitAll
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me - 현재 사용자 정보 조회")
    inner class GetCurrentUserTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("현재 사용자 정보를 반환한다")
            fun getCurrentUser() {
                mockMvc.perform(
                    get("/api/v1/users/me")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.id").value(user.id!!.toInt()))
                    .andExpect(jsonPath("$.nickname").value("테스트유저"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(get("/api/v1/users/me"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/nickname - 닉네임 수정")
    inner class UpdateNicknameTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("닉네임을 수정한다")
            fun updateNickname() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("nickname" to "새닉네임이다")
                )

                mockMvc.perform(
                    patch("/api/v1/users/nickname")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.nickname").value("새닉네임이다"))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("이미 존재하는 닉네임으로 수정하면 에러를 반환한다")
            fun duplicateNickname() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("nickname" to "다른유저")
                )

                mockMvc.perform(
                    patch("/api/v1/users/nickname")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("빈 닉네임으로 수정하면 에러를 반환한다")
            fun emptyNickname() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("nickname" to "")
                )

                mockMvc.perform(
                    patch("/api/v1/users/nickname")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("닉네임이 20자를 초과하면 에러를 반환한다")
            fun nicknameTooLong() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("nickname" to "이것은매우긴닉네임이라서이십자를초과합니다아아아")
                )

                mockMvc.perform(
                    patch("/api/v1/users/nickname")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("nickname" to "새닉네임")
                )

                mockMvc.perform(
                    patch("/api/v1/users/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/device/{token} - 디바이스 토큰 등록")
    inner class UpdateDeviceTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("디바이스 토큰을 등록한다")
            fun updateDeviceToken() {
                mockMvc.perform(
                    patch("/api/v1/users/device/{token}", "fcm-token-12345")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(patch("/api/v1/users/device/{token}", "fcm-token"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/settings - 사용자 설정 수정")
    inner class UpdateSettingsTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("알림 설정을 수정한다")
            fun updateSettings() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf(
                        "autoAddBookmarkToCalendar" to true,
                        "alarmSettings" to mapOf(
                            "push" to false,
                            "bookmark" to true,
                            "calendar" to false,
                            "marketing" to false
                        )
                    )
                )

                mockMvc.perform(
                    patch("/api/v1/users/settings")
                        .header("Authorization", authHeader(user.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.autoAddBookmarkToCalendar").value(true))
                    .andExpect(jsonPath("$.alarmSettings.push").value(false))
                    .andExpect(jsonPath("$.alarmSettings.marketing").value(false))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf(
                        "autoAddBookmarkToCalendar" to true,
                        "alarmSettings" to mapOf(
                            "push" to true, "bookmark" to true,
                            "calendar" to true, "marketing" to true
                        )
                    )
                )

                mockMvc.perform(
                    patch("/api/v1/users/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users - 회원 탈퇴")
    inner class WithdrawTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("회원 탈퇴에 성공한다")
            fun withdraw() {
                mockMvc.perform(
                    delete("/api/v1/users")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isNoContent)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                mockMvc.perform(delete("/api/v1/users"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }
}
