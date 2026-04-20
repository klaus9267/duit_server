package duit.server.domain.job.controller

import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.SalaryType
import duit.server.domain.job.entity.WorkRegion
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("채용공고 API 통합 테스트")
class JobPostingControllerIntegrationTest : IntegrationTestSupport() {

    private lateinit var user1: User

    @BeforeEach
    fun setUp() {
        user1 = TestFixtures.user(providerId = "job-test-user-1", nickname = "잡테스트유저")
        entityManager.persist(user1)

        entityManager.persist(
            TestFixtures.jobPosting(
                title = "서울 정규직 간호사",
                companyName = "서울대병원",
                workRegion = WorkRegion.SEOUL,
                employmentType = EmploymentType.FULL_TIME,
                educationLevel = EducationLevel.BACHELOR,
                salaryMin = 5000,
                salaryMax = 6000,
                salaryType = SalaryType.ANNUAL,
                closeType = CloseType.FIXED,
                expiresAt = java.time.LocalDateTime.now().plusDays(5),
                isActive = true,
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "부산 계약직 간호사",
                companyName = "부산병원",
                workRegion = WorkRegion.BUSAN,
                employmentType = EmploymentType.CONTRACT,
                educationLevel = EducationLevel.ASSOCIATE,
                salaryMin = 3000,
                salaryMax = 3500,
                salaryType = SalaryType.ANNUAL,
                closeType = CloseType.FIXED,
                expiresAt = java.time.LocalDateTime.now().plusDays(10),
                isActive = true,
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "경기 파트타임 간호조무사",
                companyName = "경기의원",
                workRegion = WorkRegion.GYEONGGI,
                employmentType = EmploymentType.PART_TIME,
                educationLevel = EducationLevel.HIGH_SCHOOL,
                salaryMin = 1500,
                salaryMax = null,
                salaryType = SalaryType.MONTHLY,
                closeType = CloseType.ON_HIRE,
                expiresAt = null,
                isActive = true,
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "서울 상시채용 간호사",
                companyName = "강남클리닉",
                workRegion = WorkRegion.SEOUL,
                employmentType = EmploymentType.FULL_TIME,
                educationLevel = EducationLevel.ASSOCIATE,
                salaryMin = 4000,
                salaryMax = 4500,
                salaryType = SalaryType.ANNUAL,
                closeType = CloseType.ONGOING,
                expiresAt = null,
                isActive = true,
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "대구 정규직 간호사",
                companyName = "대구병원",
                workRegion = WorkRegion.DAEGU,
                employmentType = EmploymentType.FULL_TIME,
                educationLevel = EducationLevel.BACHELOR,
                salaryMin = 4500,
                salaryMax = 5500,
                salaryType = SalaryType.ANNUAL,
                closeType = CloseType.FIXED,
                expiresAt = java.time.LocalDateTime.now().plusDays(20),
                isActive = true,
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "인천 계약직 간호사",
                companyName = "인천병원",
                workRegion = WorkRegion.INCHEON,
                employmentType = EmploymentType.CONTRACT,
                educationLevel = EducationLevel.ASSOCIATE,
                salaryMin = 3200,
                salaryMax = 3800,
                salaryType = SalaryType.ANNUAL,
                closeType = CloseType.FIXED,
                expiresAt = java.time.LocalDateTime.now().plusDays(15),
                isActive = true,
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "광주 시급 간호조무사",
                companyName = "광주의원",
                workRegion = WorkRegion.GWANGJU,
                employmentType = EmploymentType.PART_TIME,
                educationLevel = EducationLevel.HIGH_SCHOOL,
                salaryMin = 12,
                salaryMax = null,
                salaryType = SalaryType.HOURLY,
                closeType = CloseType.ON_HIRE,
                expiresAt = null,
                isActive = true,
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "비활성 공고",
                companyName = "폐업병원",
                workRegion = WorkRegion.SEOUL,
                isActive = false,
                closeType = CloseType.FIXED,
                expiresAt = java.time.LocalDateTime.now().minusDays(1),
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "대전 정규직 간호사",
                companyName = "대전병원",
                workRegion = WorkRegion.DAEJEON,
                employmentType = EmploymentType.FULL_TIME,
                educationLevel = EducationLevel.BACHELOR,
                salaryMin = 4800,
                salaryMax = 5200,
                salaryType = SalaryType.ANNUAL,
                closeType = CloseType.FIXED,
                expiresAt = java.time.LocalDateTime.now().plusDays(3),
                isActive = true,
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "울산 정규직 간호사",
                companyName = "울산병원",
                workRegion = WorkRegion.ULSAN,
                employmentType = EmploymentType.FULL_TIME,
                educationLevel = EducationLevel.BACHELOR,
                salaryMin = 5500,
                salaryMax = 6500,
                salaryType = SalaryType.ANNUAL,
                closeType = CloseType.FIXED,
                expiresAt = java.time.LocalDateTime.now().plusDays(25),
                isActive = true,
            )
        )

        entityManager.flush()
        entityManager.clear()
    }

    @Nested
    @DisplayName("GET /api/v1/job-postings")
    inner class GetJobPostings {

        @Nested
        @DisplayName("성공 케이스")
        inner class SuccessTests {

            @Test
            fun `기본 목록 조회 - active 공고만 반환`() {
                mockMvc.perform(get("/api/v1/job-postings"))
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.pageInfo.pageSize").isNumber)
                    .andExpect(jsonPath("$.content[0].wantedTitle").exists())
            }

            @Test
            fun `페이지네이션 - size 지정`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("size", "3")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.pageInfo.hasNext").value(true))
            }

            @Test
            fun `커서 페이지네이션 - 두 번째 페이지 조회`() {
                val firstPageResult = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("size", "3")
                )
                    .andExpect(status().isOk)
                    .andReturn()

                val body = firstPageResult.response.contentAsString
                val nextCursor = com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(body)
                    .get("pageInfo")
                    .get("nextCursor")
                    .asText()

                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("size", "3")
                        .param("cursor", nextCursor)
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(3))
            }

            @Test
            fun `지역 필터 - workRegions 다중 선택`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("workRegions", "SEOUL", "BUSAN")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(3))
            }

            @Test
            fun `고용형태 필터 - employmentTypes`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("employmentTypes", "CONTRACT")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(2))
            }

            @Test
            fun `학력 필터 - educationLevel`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("educationLevel", "BACHELOR")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(4))
            }

            @Test
            fun `급여유형 필터 - salaryType`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("salaryType", "HOURLY")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].company.corpNm").value("광주의원"))
            }

            @Test
            fun `마감유형 필터 - closeTypes`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("closeTypes", "ON_HIRE", "ONGOING")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(3))
            }

            @Test
            fun `검색 - 제목으로 검색`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("searchKeyword", "서울")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(2))
            }

            @Test
            fun `검색 - 회사명으로 검색`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("searchKeyword", "대구병원")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].company.corpNm").value("대구병원"))
            }

            @Test
            fun `기본 정렬은 id 역순이다`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content[0].company.corpNm").value("울산병원"))
            }

            @Test
            fun `비활성 공고 제외 확인`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("searchKeyword", "비활성")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(0))
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/job-postings/{id}")
    inner class GetJobPostingDetail {

        @Test
        fun `상세 조회 성공`() {
            val posting = TestFixtures.jobPosting(
                title = "상세조회 테스트 공고",
                companyName = "상세조회병원",
            )
            entityManager.persist(posting)
            entityManager.flush()
            entityManager.clear()

            mockMvc.perform(get("/api/v1/job-postings/{id}", posting.id!!))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(posting.id!!.toInt()))
                .andExpect(jsonPath("$.wantedTitle").value("상세조회 테스트 공고"))
                .andExpect(jsonPath("$.company.corpNm").value("상세조회병원"))
                .andExpect(jsonPath("$.isBookmarked").value(false))
        }

        @Test
        fun `존재하지 않는 ID 조회 시 404`() {
            mockMvc.perform(get("/api/v1/job-postings/{id}", 999999L))
                .andDo(print())
                .andExpect(status().isNotFound)
        }
    }
}
