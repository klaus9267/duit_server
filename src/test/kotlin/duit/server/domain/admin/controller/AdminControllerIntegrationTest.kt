package duit.server.domain.admin.controller

import duit.server.domain.admin.entity.Admin
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Admin API 통합 테스트")
class AdminControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var firstAdminUser: User
    private lateinit var secondAdminUser: User
    private lateinit var normalUser: User
    private lateinit var firstAdmin: Admin
    private lateinit var secondAdmin: Admin

    @BeforeEach
    fun setUp() {
        firstAdminUser = TestFixtures.user(nickname = "첫관리자", providerId = "admin-p1", email = "first@example.com")
        secondAdminUser = TestFixtures.user(nickname = "둘째관리자", providerId = "admin-p2", email = null)
        normalUser = TestFixtures.user(nickname = "일반유저", providerId = "normal-p")
        listOf(firstAdminUser, secondAdminUser, normalUser).forEach(entityManager::persist)

        firstAdmin = TestFixtures.admin(user = firstAdminUser, adminId = "first-admin", password = "\$2a\$10\$hashed-first")
        secondAdmin = TestFixtures.admin(user = secondAdminUser, adminId = "second-admin", password = "\$2a\$10\$hashed-second")
        listOf(firstAdmin, secondAdmin).forEach(entityManager::persist)

        entityManager.flush()
        entityManager.clear()
    }

    private fun adminCount(): Long =
        entityManager.createQuery("SELECT COUNT(a) FROM Admin a", Long::class.javaObjectType).singleResult

    private fun getAdmins(callerId: Long?, vararg params: Pair<String, String>): ResultActions =
        mockMvc.perform(
            get("/api/v1/admins")
                .apply {
                    callerId?.let { header("Authorization", authHeader(it)) }
                    params.forEach { (name, value) -> param(name, value) }
                }
        ).andDo(print())

    private fun revoke(callerId: Long?, id: Any): ResultActions =
        mockMvc.perform(
            delete("/api/v1/admins/{id}", id)
                .apply { callerId?.let { header("Authorization", authHeader(it)) } }
        ).andDo(print())

    @Nested
    @DisplayName("GET /api/v1/admins - 관리자 목록 조회")
    inner class GetAdminsTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("관리자는 관리자 목록을 최신 등록 순으로 조회한다")
            fun listsAdminsNewestFirst() {
                getAdmins(firstAdminUser.id)
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(secondAdmin.id!!.toInt()))
                    .andExpect(jsonPath("$.content[0].adminId").value("second-admin"))
                    .andExpect(jsonPath("$.content[0].userId").value(secondAdminUser.id!!.toInt()))
                    .andExpect(jsonPath("$.content[0].nickname").value("둘째관리자"))
                    .andExpect(jsonPath("$.content[0].email").doesNotExist())
                    .andExpect(jsonPath("$.content[0].createdAt").isString)
                    .andExpect(jsonPath("$.content[1].id").value(firstAdmin.id!!.toInt()))
                    .andExpect(jsonPath("$.content[1].email").value("first@example.com"))
                    .andExpect(jsonPath("$.pageInfo.totalElements").value(2))
            }

            @Test
            @DisplayName("응답에 비밀번호와 해시, 사용자의 외부 인증 식별자는 포함되지 않는다")
            fun doesNotExposeSensitiveFields() {
                getAdmins(firstAdminUser.id)
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content[0].password").doesNotExist())
                    .andExpect(jsonPath("$.content[0].user").doesNotExist())
                    .andExpect(jsonPath("$.content[0].providerId").doesNotExist())
                    .andExpect(content().string(not(containsString("hashed"))))
                    .andExpect(content().string(not(containsString("\$2a\$"))))
                    .andExpect(content().string(not(containsString("admin-p1"))))
            }

            @Test
            @DisplayName("페이징하여 조회한다")
            fun paginates() {
                getAdmins(firstAdminUser.id, "page" to "0", "size" to "1")
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].adminId").value("second-admin"))
                    .andExpect(jsonPath("$.pageInfo.totalElements").value(2))
                    .andExpect(jsonPath("$.pageInfo.totalPages").value(2))

                getAdmins(firstAdminUser.id, "page" to "1", "size" to "1")
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].adminId").value("first-admin"))
            }

            @Test
            @DisplayName("sortDirection=ASC 이면 오래된 등록 순으로 조회한다")
            fun sortsAscending() {
                getAdmins(firstAdminUser.id, "sortDirection" to "ASC")
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content[0].adminId").value("first-admin"))
                    .andExpect(jsonPath("$.content[1].adminId").value("second-admin"))
            }

            @Test
            @DisplayName("정렬 필드를 지정해도 무시하고 항상 등록 순으로 정렬한다")
            fun ignoresSortField() {
                getAdmins(firstAdminUser.id, "field" to "NAME", "sort" to "password,asc")
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content[0].adminId").value("second-admin"))
            }

            @Test
            @DisplayName("과도한 페이지 크기와 음수 페이지는 보정되어 정상 응답한다")
            fun clampsPaging() {
                getAdmins(firstAdminUser.id, "page" to "-5", "size" to "100000")
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.pageInfo.pageNumber").value(0))
                    .andExpect(jsonPath("$.pageInfo.pageSize").value(100))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환한다")
            fun unauthorized() {
                getAdmins(null).andExpect(status().isUnauthorized)
            }

            @Test
            @DisplayName("관리자가 아닌 사용자는 403을 반환한다")
            fun forbiddenForNormalUser() {
                getAdmins(normalUser.id)
                    .andExpect(status().isForbidden)
                    .andExpect(content().string(not(containsString("first-admin"))))
            }

            @Test
            @DisplayName("지원하지 않는 sortDirection 값이면 400을 반환한다")
            fun invalidSortDirection() {
                getAdmins(firstAdminUser.id, "sortDirection" to "SIDEWAYS").andExpect(status().isBadRequest)
            }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admins/{id} - 관리자 해제")
    inner class RevokeAdminTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("다른 관리자를 해제하면 204를 반환하고 목록에서 사라지며 사용자 계정은 유지된다")
            fun revokeSuccess() {
                revoke(firstAdminUser.id, secondAdmin.id!!).andExpect(status().isNoContent)

                assertEquals(1L, adminCount())
                assertNotNull(entityManager.find(User::class.java, secondAdminUser.id))
                getAdmins(firstAdminUser.id)
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].adminId").value("first-admin"))
            }

            @Test
            @DisplayName("해제된 사용자의 기존 토큰은 즉시 관리자 권한을 잃는다")
            fun revokedAdminLosesAccessImmediately() {
                val revokedUserHeader = authHeader(secondAdminUser.id!!)
                mockMvc.perform(get("/api/v1/admins").header("Authorization", revokedUserHeader))
                    .andExpect(status().isOk)

                revoke(firstAdminUser.id, secondAdmin.id!!).andExpect(status().isNoContent)

                mockMvc.perform(get("/api/v1/admins").header("Authorization", revokedUserHeader))
                    .andExpect(status().isForbidden)
                mockMvc.perform(delete("/api/v1/admins/{id}", firstAdmin.id).header("Authorization", revokedUserHeader))
                    .andExpect(status().isForbidden)
                assertEquals(1L, adminCount())
            }

            @Test
            @DisplayName("해제된 사용자는 다른 관리자가 다시 등록할 수 있다")
            fun canBeRegisteredAgainAfterRevoke() {
                revoke(firstAdminUser.id, secondAdmin.id!!).andExpect(status().isNoContent)

                mockMvc.perform(
                    post("/api/v1/admin/auth/register")
                        .header("Authorization", authHeader(firstAdminUser.id!!))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf("userId" to secondAdminUser.id, "adminId" to "second-admin", "password" to "newpassword1234")
                            )
                        )
                ).andExpect(status().isCreated)

                assertEquals(2L, adminCount())
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("본인의 관리자 권한은 해제할 수 없다")
            fun cannotRevokeSelf() {
                revoke(firstAdminUser.id, firstAdmin.id!!).andExpect(status().isBadRequest)

                assertEquals(2L, adminCount())
            }

            @Test
            @DisplayName("존재하지 않는 관리자를 해제하면 404를 반환한다")
            fun notFound() {
                revoke(firstAdminUser.id, 999_999L).andExpect(status().isNotFound)

                assertEquals(2L, adminCount())
            }

            @Test
            @DisplayName("id 가 숫자가 아니면 400을 반환한다")
            fun invalidId() {
                revoke(firstAdminUser.id, "abc").andExpect(status().isBadRequest)
            }

            @Test
            @DisplayName("인증 없이 접근하면 401을 반환하고 아무것도 삭제되지 않는다")
            fun unauthorized() {
                revoke(null, secondAdmin.id!!).andExpect(status().isUnauthorized)

                assertEquals(2L, adminCount())
            }

            @Test
            @DisplayName("관리자가 아닌 사용자는 403을 반환하고 아무것도 삭제되지 않는다")
            fun forbiddenForNormalUser() {
                revoke(normalUser.id, secondAdmin.id!!).andExpect(status().isForbidden)

                assertEquals(2L, adminCount())
            }
        }
    }
}
