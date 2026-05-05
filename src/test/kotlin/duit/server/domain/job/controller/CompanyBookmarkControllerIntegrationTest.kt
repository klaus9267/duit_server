package duit.server.domain.job.controller

import duit.server.domain.job.entity.Company
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("회사 북마크 API 통합 테스트")
class CompanyBookmarkControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user: User
    private lateinit var company: Company

    @BeforeEach
    fun setUp() {
        user = TestFixtures.user(nickname = "회사북마크유저", providerId = "company-bookmark-user")
        entityManager.persist(user)

        company = TestFixtures.company(corpNm = "북마크대상회사")
        entityManager.persist(company)

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("POST /api/v1/company-bookmarks/{companyId} - 회사 북마크 토글")
    inner class ToggleBookmark {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("처음 호출하면 북마크가 추가되고 isBookmarked=true 를 반환한다")
            fun addBookmark() {
                mockMvc.perform(
                    post("/api/v1/company-bookmarks/{id}", company.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.companyId").value(company.id!!.toInt()))
                    .andExpect(jsonPath("$.isBookmarked").value(true))
            }

            @Test
            @DisplayName("두 번 호출하면 북마크가 해제되어 isBookmarked=false 를 반환한다")
            fun removeBookmark() {
                mockMvc.perform(
                    post("/api/v1/company-bookmarks/{id}", company.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.isBookmarked").value(true))

                mockMvc.perform(
                    post("/api/v1/company-bookmarks/{id}", company.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.companyId").value(company.id!!.toInt()))
                    .andExpect(jsonPath("$.isBookmarked").value(false))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("존재하지 않는 companyId 로 토글 시 404 를 반환한다")
            fun notFound() {
                mockMvc.perform(
                    post("/api/v1/company-bookmarks/{id}", 999_999L)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isNotFound)
            }

            @Test
            @DisplayName("미인증 요청은 401 을 반환한다")
            fun unauthorized() {
                mockMvc.perform(post("/api/v1/company-bookmarks/{id}", company.id!!))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }
}
