package duit.server.domain.job.controller

import duit.server.domain.job.entity.JobPosting
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("채용공고 북마크 API 통합 테스트")
class JobBookmarkControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user1: User
    private lateinit var activePosting: JobPosting
    private lateinit var inactivePosting: JobPosting

    @BeforeEach
    fun setUp() {
        user1 = TestFixtures.user(providerId = "bookmark-job-user-1", nickname = "북마크잡유저")
        entityManager.persist(user1)

        activePosting = TestFixtures.jobPosting(
            title = "활성 채용공고",
            companyName = "활성병원",
            isActive = true,
        )
        entityManager.persist(activePosting)

        inactivePosting = TestFixtures.jobPosting(
            title = "비활성 채용공고",
            companyName = "비활성병원",
            isActive = false,
        )
        entityManager.persist(inactivePosting)

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("POST /api/v1/job-bookmarks/{id}")
    inner class ToggleBookmark {

        @Test
        fun `북마크 추가 성공`() {
            mockMvc.perform(
                post("/api/v1/job-bookmarks/{id}", activePosting.id!!)
                    .header("Authorization", authHeader(user1.id!!))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.jobPostingId").value(activePosting.id!!.toInt()))
                .andExpect(jsonPath("$.isBookmarked").value(true))
        }

        @Test
        fun `북마크 해제 성공`() {
            mockMvc.perform(
                post("/api/v1/job-bookmarks/{id}", activePosting.id!!)
                    .header("Authorization", authHeader(user1.id!!))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isBookmarked").value(true))

            mockMvc.perform(
                post("/api/v1/job-bookmarks/{id}", activePosting.id!!)
                    .header("Authorization", authHeader(user1.id!!))
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.jobPostingId").value(activePosting.id!!.toInt()))
                .andExpect(jsonPath("$.isBookmarked").value(false))
        }

        @Test
        fun `비활성 공고 북마크 시 400`() {
            mockMvc.perform(
                post("/api/v1/job-bookmarks/{id}", inactivePosting.id!!)
                    .header("Authorization", authHeader(user1.id!!))
            )
                .andDo(print())
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `미인증 사용자 북마크 시 401`() {
            mockMvc.perform(
                post("/api/v1/job-bookmarks/{id}", activePosting.id!!)
            )
                .andDo(print())
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    @DisplayName("북마크 필터 연동")
    inner class BookmarkFilter {

        @Test
        fun `북마크한 공고만 조회`() {
            val posting2 = TestFixtures.jobPosting(
                title = "북마크할 공고",
                companyName = "북마크병원",
                isActive = true,
            )
            entityManager.persist(posting2)
            entityManager.persist(TestFixtures.jobBookmark(user = user1, jobPosting = posting2))
            entityManager.flush()
            entityManager.clear()

            mockMvc.perform(
                get("/api/v1/job-postings")
                    .header("Authorization", authHeader(user1.id!!))
                    .param("bookmarked", "true")
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].company.corpNm").value("북마크병원"))
        }

        @Test
        fun `목록 조회 시 isBookmarked 포함 확인`() {
            entityManager.persist(TestFixtures.jobBookmark(user = user1, jobPosting = activePosting))
            entityManager.flush()
            entityManager.clear()

            mockMvc.perform(
                get("/api/v1/job-postings")
                    .header("Authorization", authHeader(user1.id!!))
                    .param("searchKeyword", "활성 채용공고")
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content[0].isBookmarked").value(true))
        }
    }
}
