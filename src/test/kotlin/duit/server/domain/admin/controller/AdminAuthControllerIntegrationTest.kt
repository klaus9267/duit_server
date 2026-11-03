package duit.server.domain.admin.controller

import duit.server.domain.admin.entity.Admin
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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

        private fun register(callerId: Long?, body: Map<String, Any?>) =
            mockMvc.perform(
                post("/api/v1/admin/auth/register")
                    .apply { callerId?.let { header("Authorization", authHeader(it)) } }
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body))
            ).andDo(print())

        private fun adminCount(): Long =
            entityManager.createQuery("SELECT COUNT(a) FROM Admin a", Long::class.javaObjectType).singleResult

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("관리자가 지정한 사용자를 관리자로 등록한다")
            fun registerSuccess() {
                register(
                    adminUser.id,
                    mapOf("userId" to registerUser.id, "adminId" to "newadmin", "password" to "newpassword1234")
                )
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.adminId").value("newadmin"))
                    .andExpect(jsonPath("$.userId").value(registerUser.id!!.toInt()))
                    .andExpect(jsonPath("$.nickname").value(registerUser.nickname))
                    .andExpect(jsonPath("$.password").doesNotExist())
            }

            @Test
            @DisplayName("등록된 계정으로 관리자 로그인할 수 있고 비밀번호는 해시로 저장된다")
            fun registeredAdminCanLogin() {
                register(
                    adminUser.id,
                    mapOf("userId" to registerUser.id, "adminId" to "newadmin", "password" to "newpassword1234")
                ).andExpect(status().isCreated)

                val stored = entityManager
                    .createQuery("SELECT a.password FROM Admin a WHERE a.adminId = 'newadmin'", String::class.java)
                    .singleResult
                assertNotEquals("newpassword1234", stored)
                assertTrue(passwordEncoder.matches("newpassword1234", stored))

                mockMvc.perform(
                    post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mapOf("adminId" to "newadmin", "password" to "newpassword1234")))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.accessToken").isString)
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                val before = adminCount()

                register(null, mapOf("userId" to registerUser.id, "adminId" to "newadmin", "password" to "newpassword1234"))
                    .andExpect(status().isUnauthorized)

                assertEquals(before, adminCount())
            }

            @Test
            @DisplayName("관리자가 아닌 사용자는 403을 반환하고 아무것도 생성되지 않는다 (대상: 다른 사용자)")
            fun forbiddenForNonAdminTargetingOther() {
                val before = adminCount()

                register(
                    registerUser.id,
                    mapOf("userId" to adminUser.id, "adminId" to "newadmin", "password" to "newpassword1234")
                ).andExpect(status().isForbidden)

                assertEquals(before, adminCount())
            }

            @Test
            @DisplayName("관리자가 아닌 사용자는 403을 반환하고 아무것도 생성되지 않는다 (대상: 본인)")
            fun forbiddenForNonAdminTargetingSelf() {
                val before = adminCount()

                register(
                    registerUser.id,
                    mapOf("userId" to registerUser.id, "adminId" to "newadmin", "password" to "newpassword1234")
                ).andExpect(status().isForbidden)

                assertEquals(before, adminCount())
                mockMvc.perform(get("/api/v1/admins").header("Authorization", authHeader(registerUser.id!!)))
                    .andExpect(status().isForbidden)
            }

            @Test
            @DisplayName("존재하지 않는 사용자를 지정하면 404를 반환한다")
            fun targetUserNotFound() {
                register(
                    adminUser.id,
                    mapOf("userId" to 999_999L, "adminId" to "newadmin", "password" to "newpassword1234")
                ).andExpect(status().isNotFound)
            }

            @Test
            @DisplayName("이미 관리자인 사용자를 지정하면 409를 반환한다")
            fun targetUserAlreadyAdmin() {
                register(
                    adminUser.id,
                    mapOf("userId" to adminUser.id, "adminId" to "another", "password" to "newpassword1234")
                ).andExpect(status().isConflict)
            }

            @Test
            @DisplayName("이미 존재하는 adminId로 등록하면 에러를 반환한다")
            fun duplicateAdminId() {
                register(
                    adminUser.id,
                    mapOf("userId" to registerUser.id, "adminId" to "testadmin", "password" to "newpassword1234")
                ).andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("adminId가 4자 미만이면 에러를 반환한다")
            fun adminIdTooShort() {
                register(
                    adminUser.id,
                    mapOf("userId" to registerUser.id, "adminId" to "ab", "password" to "newpassword1234")
                ).andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("password가 8자 미만이면 에러를 반환한다")
            fun passwordTooShort() {
                register(
                    adminUser.id,
                    mapOf("userId" to registerUser.id, "adminId" to "newadmin", "password" to "short")
                ).andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("userId가 없으면 에러를 반환한다")
            fun userIdMissing() {
                register(adminUser.id, mapOf("adminId" to "newadmin", "password" to "newpassword1234"))
                    .andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("userId가 양수가 아니면 에러를 반환한다")
            fun userIdNotPositive() {
                register(
                    adminUser.id,
                    mapOf("userId" to 0, "adminId" to "newadmin", "password" to "newpassword1234")
                ).andExpect(status().isBadRequest)
            }
        }
    }
}
