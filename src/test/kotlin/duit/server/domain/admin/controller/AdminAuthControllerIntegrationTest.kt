package duit.server.domain.admin.controller

import duit.server.domain.admin.entity.Admin
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Admin Auth API 통합 테스트")
class AdminAuthControllerIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminUser: User
    private lateinit var registerUser: User
    private lateinit var admin: Admin

    @BeforeEach
    fun setUp() {
        adminUser = TestFixtures.user(nickname = "관리자유저", providerId = "admin-provider")
        entityManager.persist(adminUser)

        registerUser = TestFixtures.user(nickname = "등록용유저", providerId = "register-provider", email = "register@example.com")
        entityManager.persist(registerUser)

        admin = TestFixtures.admin(
            user = adminUser,
            adminId = "testadmin",
            password = passwordEncoder.encode("password1234")
        )
        entityManager.persist(admin)

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("POST /api/v1/admin/auth/login - 관리자 로그인")
    inner class LoginTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("올바른 아이디와 비밀번호로 로그인한다")
            fun loginSuccess() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "testadmin", "password" to "password1234")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").isString)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("존재하지 않는 아이디로 로그인하면 에러를 반환한다")
            fun wrongAdminId() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "wrongadmin", "password" to "password1234")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("잘못된 비밀번호로 로그인하면 에러를 반환한다")
            fun wrongPassword() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "testadmin", "password" to "wrongpassword")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("차단된 IP에서 로그인하면 에러를 반환한다")
            fun bannedIp() {
                val bannedIp = TestFixtures.bannedIp(
                    ipAddress = "127.0.0.1",
                    failureCount = 5,
                    isBanned = true
                )
                entityManager.persist(bannedIp)
                entityManager.flush()
                entityManager.clear()

                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "testadmin", "password" to "password1234")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isConflict)
            }

            @Test
            @DisplayName("adminId가 빈 값이면 에러를 반환한다")
            fun emptyAdminId() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "", "password" to "password1234")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("password가 빈 값이면 에러를 반환한다")
            fun emptyPassword() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "testadmin", "password" to "")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/auth/register - 관리자 등록")
    inner class RegisterTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("새로운 관리자 계정을 등록한다")
            fun registerSuccess() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "newadmin", "password" to "newpassword1234")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/register")
                        .header("Authorization", authHeader(registerUser.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.adminId").value("newadmin"))
                    .andExpect(jsonPath("$.userId").value(registerUser.id!!.toInt()))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "newadmin", "password" to "newpassword1234")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }

            @Test
            @DisplayName("이미 존재하는 adminId로 등록하면 에러를 반환한다")
            fun duplicateAdminId() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "testadmin", "password" to "newpassword1234")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/register")
                        .header("Authorization", authHeader(registerUser.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("adminId가 4자 미만이면 에러를 반환한다")
            fun adminIdTooShort() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "ab", "password" to "newpassword1234")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/register")
                        .header("Authorization", authHeader(registerUser.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("password가 8자 미만이면 에러를 반환한다")
            fun passwordTooShort() {
                val requestBody = objectMapper.writeValueAsString(
                    mapOf("adminId" to "newadmin", "password" to "short")
                )

                mockMvc.perform(
                    post("/api/v1/admin/auth/register")
                        .header("Authorization", authHeader(registerUser.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                    .andDo(print())
                    .andExpect(status().isBadRequest)
            }
        }
    }
}
