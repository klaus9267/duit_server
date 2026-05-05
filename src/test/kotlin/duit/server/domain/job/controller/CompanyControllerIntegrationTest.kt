package duit.server.domain.job.controller

import duit.server.domain.job.entity.Company
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Company API 통합 테스트")
class CompanyControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user: User
    private lateinit var companyA: Company
    private lateinit var companyB: Company
    private lateinit var companyC: Company

    @BeforeEach
    fun setUp() {
        user = TestFixtures.user(nickname = "회사조회유저", providerId = "company-detail-user")
        entityManager.persist(user)

        companyA = TestFixtures.company(corpNm = "병원A", businessNumber = "comp-A-${System.nanoTime()}")
        companyB = TestFixtures.company(corpNm = "병원B", businessNumber = "comp-B-${System.nanoTime()}")
        companyC = TestFixtures.company(corpNm = "병원C", businessNumber = "comp-C-${System.nanoTime()}")
        entityManager.persist(companyA)
        entityManager.persist(companyB)
        entityManager.persist(companyC)

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("GET /api/v1/companies/{companyId} - 회사 상세 조회")
    inner class GetCompanyDetailTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("비로그인 상태에서도 상세 조회되며 isBookmarked 는 false 다")
            fun publicAccess() {
                mockMvc.perform(get("/api/v1/companies/{id}", companyA.id!!))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.id").value(companyA.id!!.toInt()))
                    .andExpect(jsonPath("$.corpNm").value("병원A"))
                    .andExpect(jsonPath("$.isBookmarked").value(false))
            }

            @Test
            @DisplayName("북마크한 회사 조회 시 isBookmarked 가 true 다")
            fun bookmarkedFlagTrue() {
                entityManager.persist(TestFixtures.companyBookmark(user = user, company = companyA))
                entityManager.flush()
                entityManager.clear()

                mockMvc.perform(
                    get("/api/v1/companies/{id}", companyA.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.isBookmarked").value(true))
            }

            @Test
            @DisplayName("북마크하지 않은 회사 조회 시 isBookmarked 는 false 다")
            fun bookmarkedFlagFalse() {
                mockMvc.perform(
                    get("/api/v1/companies/{id}", companyB.id!!)
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.isBookmarked").value(false))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("존재하지 않는 companyId 는 404 를 반환한다")
            fun notFound() {
                mockMvc.perform(get("/api/v1/companies/{id}", 999_999L))
                    .andDo(print())
                    .andExpect(status().isNotFound)
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/companies/bookmarked - 북마크한 회사 목록 조회")
    inner class GetBookmarkedTests {

        @Nested
        @DisplayName("성공")
        inner class Success {

            @Test
            @DisplayName("북마크 순(최신 → 과거)으로 회사 목록을 반환한다")
            fun returnsByMostRecentBookmark() {
                entityManager.persist(TestFixtures.companyBookmark(user = user, company = companyA))
                entityManager.flush()
                entityManager.persist(TestFixtures.companyBookmark(user = user, company = companyC))
                entityManager.flush()
                entityManager.clear()

                mockMvc.perform(
                    get("/api/v1/companies/bookmarked")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$").value(hasSize<Any>(2)))
                    .andExpect(jsonPath("$[0].corpNm").value("병원C"))
                    .andExpect(jsonPath("$[0].isBookmarked").value(true))
                    .andExpect(jsonPath("$[1].corpNm").value("병원A"))
                    .andExpect(jsonPath("$[1].isBookmarked").value(true))
            }

            @Test
            @DisplayName("북마크가 없으면 빈 목록을 반환한다")
            fun returnsEmptyList() {
                mockMvc.perform(
                    get("/api/v1/companies/bookmarked")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$").value(hasSize<Any>(0)))
            }

            @Test
            @DisplayName("다른 사용자의 북마크는 노출되지 않는다")
            fun isolatedPerUser() {
                val other = TestFixtures.user(nickname = "다른유저", providerId = "other-user")
                entityManager.persist(other)
                entityManager.persist(TestFixtures.companyBookmark(user = other, company = companyA))
                entityManager.persist(TestFixtures.companyBookmark(user = user, company = companyB))
                entityManager.flush()
                entityManager.clear()

                mockMvc.perform(
                    get("/api/v1/companies/bookmarked")
                        .header("Authorization", authHeader(user.id!!))
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$").value(hasSize<Any>(1)))
                    .andExpect(jsonPath("$[0].corpNm").value("병원B"))
            }
        }

        @Nested
        @DisplayName("실패")
        inner class Failure {

            @Test
            @DisplayName("미인증 요청은 401 을 반환한다")
            fun unauthorized() {
                mockMvc.perform(get("/api/v1/companies/bookmarked"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized)
            }
        }
    }
}
