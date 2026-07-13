package duit.server.domain.job.controller

import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.SalaryType
import duit.server.domain.job.entity.WorkRegion
import duit.server.domain.user.entity.User
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Base64

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
                salaryMin = 50_000_000,
                salaryMax = 60_000_000,
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
                salaryMin = 30_000_000,
                salaryMax = 35_000_000,
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
                jobCategory = "간호조무사",
                jobsCd = "307500",
                workRegion = WorkRegion.GYEONGGI,
                employmentType = EmploymentType.PART_TIME,
                educationLevel = EducationLevel.HIGH_SCHOOL,
                salaryMin = 18_000_000,
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
                salaryMin = 40_000_000,
                salaryMax = 45_000_000,
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
                salaryMin = 45_000_000,
                salaryMax = 55_000_000,
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
                salaryMin = 32_000_000,
                salaryMax = 38_000_000,
                salaryType = SalaryType.ANNUAL,
                closeType = CloseType.FIXED,
                expiresAt = java.time.LocalDateTime.now().plusDays(15),
                isActive = true,
            )
        )
        entityManager.persist(
            TestFixtures.jobPosting(
                title = "광주 시급 간호사",
                companyName = "광주병원",
                workRegion = WorkRegion.GWANGJU,
                employmentType = EmploymentType.PART_TIME,
                educationLevel = EducationLevel.HIGH_SCHOOL,
                salaryMin = 30_096_000,
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
                salaryMin = 48_000_000,
                salaryMax = 52_000_000,
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
                salaryMin = 55_000_000,
                salaryMax = 65_000_000,
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
            fun `지역 필터 - 고용24 지역 코드로 원문 주소 공고 매칭`() {
                val posting = TestFixtures.jobPosting(
                    title = "고용24 서울 원문 주소 공고",
                    companyName = "서울원문병원",
                    location = "(07516)  서울특별시 강서구 양천로 31",
                    workRegion = null,
                    employmentType = EmploymentType.FULL_TIME,
                ).apply {
                    regionCd = "11500"
                    empTpCd = "10"
                    empTpNm = "기간의 정함이 없는 근로계약/ 파견근로 비희망"
                }
                entityManager.persist(posting)
                entityManager.flush()
                entityManager.clear()

                val result = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("workRegions", "SEOUL")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andReturn()

                val titles = objectMapper.readTree(result.response.contentAsString)
                    .get("content")
                    .map { it.get("wantedTitle").asText() }
                assertThat(titles).contains("고용24 서울 원문 주소 공고")
            }

            @Test
            fun `지역 필터 - 지역 코드가 없으면 우편번호 주소 원문으로 fallback 매칭`() {
                val posting = TestFixtures.jobPosting(
                    title = "고용24 서울 원문 fallback 공고",
                    companyName = "서울fallback병원",
                    location = "(07516)  서울특별시 강서구 양천로 31",
                    workRegion = null,
                    employmentType = EmploymentType.FULL_TIME,
                ).apply {
                    regionCd = null
                }
                entityManager.persist(posting)
                entityManager.flush()
                entityManager.clear()

                val result = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("workRegions", "SEOUL")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andReturn()

                val titles = objectMapper.readTree(result.response.contentAsString)
                    .get("content")
                    .map { it.get("wantedTitle").asText() }
                assertThat(titles).contains("고용24 서울 원문 fallback 공고")
            }

            @Test
            fun `지역 필터 - 지역 코드가 있으면 원문 도시명보다 코드를 우선`() {
                val posting = TestFixtures.jobPosting(
                    title = "고용24 경기 광주시 공고",
                    companyName = "경기광주병원",
                    location = "(12760)  경기도 광주시 중앙로 1",
                    workRegion = null,
                    employmentType = EmploymentType.FULL_TIME,
                ).apply {
                    regionCd = "41610"
                }
                entityManager.persist(posting)
                entityManager.flush()
                entityManager.clear()

                val gwangjuResult = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("workRegions", "GWANGJU")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andReturn()
                val gwangjuTitles = objectMapper.readTree(gwangjuResult.response.contentAsString)
                    .get("content")
                    .map { it.get("wantedTitle").asText() }
                assertThat(gwangjuTitles).doesNotContain("고용24 경기 광주시 공고")

                val gyeonggiResult = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("workRegions", "GYEONGGI")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andReturn()
                val gyeonggiTitles = objectMapper.readTree(gyeonggiResult.response.contentAsString)
                    .get("content")
                    .map { it.get("wantedTitle").asText() }
                assertThat(gyeonggiTitles).contains("고용24 경기 광주시 공고")
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
            fun `고용형태 필터 - 고용24 근로계약 원문도 매칭`() {
                val posting = TestFixtures.jobPosting(
                    title = "고용24 계약직 원문 공고",
                    companyName = "계약원문병원",
                    employmentType = null,
                ).apply {
                    empTpCd = "20"
                    empTpNm = "기간의 정함이 있는 근로계약12 개월/ 계약기간 만료 후 상용직전환검토"
                }
                entityManager.persist(posting)
                entityManager.flush()
                entityManager.clear()

                val result = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("employmentTypes", "CONTRACT")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andReturn()

                val titles = objectMapper.readTree(result.response.contentAsString)
                    .get("content")
                    .map { it.get("wantedTitle").asText() }
                assertThat(titles).contains("고용24 계약직 원문 공고")
            }

            @Test
            fun `고용형태 필터 - 고용형태 코드가 있으면 원문 문구보다 코드를 우선`() {
                val posting = TestFixtures.jobPosting(
                    title = "고용24 정규직 코드 우선 공고",
                    companyName = "정규코드병원",
                    employmentType = null,
                ).apply {
                    empTpCd = "10"
                    empTpNm = "기간의 정함이 있는 근로계약12 개월/ 계약기간 만료 후 상용직전환검토"
                }
                entityManager.persist(posting)
                entityManager.flush()
                entityManager.clear()

                val contractResult = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("employmentTypes", "CONTRACT")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andReturn()
                val contractTitles = objectMapper.readTree(contractResult.response.contentAsString)
                    .get("content")
                    .map { it.get("wantedTitle").asText() }
                assertThat(contractTitles).doesNotContain("고용24 정규직 코드 우선 공고")

                val fullTimeResult = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("employmentTypes", "FULL_TIME")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andReturn()
                val fullTimeTitles = objectMapper.readTree(fullTimeResult.response.contentAsString)
                    .get("content")
                    .map { it.get("wantedTitle").asText() }
                assertThat(fullTimeTitles).contains("고용24 정규직 코드 우선 공고")
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
                    .andExpect(jsonPath("$.content[0].company.corpNm").value("광주병원"))
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
                    .andExpect(jsonPath("$.content.length()").value(2))
            }

            @Test
            fun `간호조무사 직종코드 공고는 목록에서 제외`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("searchKeyword", "간호조무사")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content.length()").value(0))
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
            fun `기본 정렬은 고용24 최신 등록순이다`() {
                mockMvc.perform(
                    get("/api/v1/job-postings")
                )
                    .andDo(print())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content").isArray)
                    .andExpect(jsonPath("$.content[0].company.corpNm").value("울산병원"))
            }

            @Test
            fun `CREATED_AT은 최신 등록순으로 정렬한다`() {
                val result = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("field", "CREATED_AT")
                        .param("size", "3")
                )
                    .andExpect(status().isOk)
                    .andReturn()

                assertThat(responseTitles(result.response.contentAsString))
                    .containsExactly("울산 정규직 간호사", "대전 정규직 간호사", "광주 시급 간호사")
            }

            @Test
            fun `EXPIRES_AT은 마감 임박순으로 정렬한다`() {
                val result = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("field", "EXPIRES_AT")
                        .param("size", "3")
                )
                    .andExpect(status().isOk)
                    .andReturn()

                assertThat(responseTitles(result.response.contentAsString))
                    .containsExactly("대전 정규직 간호사", "서울 정규직 간호사", "부산 계약직 간호사")
            }

            @Test
            fun `SALARY는 급여 높은순으로 정렬한다`() {
                val result = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("field", "SALARY")
                        .param("size", "3")
                )
                    .andExpect(status().isOk)
                    .andReturn()

                assertThat(responseTitles(result.response.contentAsString))
                    .containsExactly("울산 정규직 간호사", "서울 정규직 간호사", "대전 정규직 간호사")
            }

            @Test
            fun `EXPIRES_AT 커서는 다음 마감 공고부터 이어서 조회한다`() {
                val firstPage = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("field", "EXPIRES_AT")
                        .param("size", "2")
                )
                    .andExpect(status().isOk)
                    .andReturn()
                val firstBody = objectMapper.readTree(firstPage.response.contentAsString)

                val secondPage = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("field", "EXPIRES_AT")
                        .param("size", "2")
                        .param("cursor", firstBody.get("pageInfo").get("nextCursor").asText())
                )
                    .andExpect(status().isOk)
                    .andReturn()

                assertThat(responseTitles(firstPage.response.contentAsString))
                    .containsExactly("대전 정규직 간호사", "서울 정규직 간호사")
                assertThat(responseTitles(secondPage.response.contentAsString))
                    .containsExactly("부산 계약직 간호사", "인천 계약직 간호사")
            }

            @Test
            fun `EXPIRES_AT은 마감일 없는 공고를 제거하지 않고 마지막에 배치한다`() {
                entityManager.persist(
                    TestFixtures.jobPosting(
                        title = "마감보존 고정 공고",
                        closeType = CloseType.FIXED,
                        expiresAt = java.time.LocalDateTime.now().plusDays(1),
                    )
                )
                entityManager.persist(
                    TestFixtures.jobPosting(
                        title = "마감보존 채용시 공고",
                        closeType = CloseType.ON_HIRE,
                        expiresAt = null,
                    )
                )
                entityManager.flush()
                entityManager.clear()

                val pages = requestTwoSingleItemPages(field = "EXPIRES_AT", searchKeyword = "마감보존")

                assertThat(responseTitles(pages.first)).containsExactly("마감보존 고정 공고")
                assertThat(responseTitles(pages.second)).containsExactly("마감보존 채용시 공고")
            }

            @Test
            fun `EXPIRES_AT 커서는 같은 마감일에도 id 역순으로 이어서 조회한다`() {
                val expiresAt = java.time.LocalDateTime.now().plusDays(1)
                listOf("동일마감 첫 공고", "동일마감 둘째 공고").forEach { title ->
                    entityManager.persist(TestFixtures.jobPosting(title = title, expiresAt = expiresAt))
                }
                entityManager.flush()
                entityManager.clear()

                val pages = requestTwoSingleItemPages(field = "EXPIRES_AT", searchKeyword = "동일마감")

                assertThat(responseTitles(pages.first)).containsExactly("동일마감 둘째 공고")
                assertThat(responseTitles(pages.second)).containsExactly("동일마감 첫 공고")
            }

            @Test
            fun `활성 상태가 남아 있어도 마감일이 지난 공고는 제외한다`() {
                entityManager.persist(
                    TestFixtures.jobPosting(
                        title = "기한경과 활성 공고",
                        expiresAt = java.time.LocalDateTime.now().minusDays(1),
                        isActive = true,
                    )
                )
                entityManager.flush()
                entityManager.clear()

                mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("searchKeyword", "기한경과")
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.content.length()").value(0))
            }

            @Test
            fun `CREATED_AT 커서는 등록 시각이 같아도 id 역순으로 이어서 조회한다`() {
                val postedAt = java.time.LocalDateTime.of(2027, 1, 1, 0, 0)
                val postings = listOf("동시등록 첫 공고", "동시등록 둘째 공고").map { title ->
                    TestFixtures.jobPosting(title = title).also(entityManager::persist)
                }
                entityManager.flush()
                entityManager.createNativeQuery(
                    "UPDATE job_postings SET posted_at = :postedAt WHERE id IN (:ids)"
                )
                    .setParameter("postedAt", postedAt)
                    .setParameter("ids", postings.map { it.id })
                    .executeUpdate()
                entityManager.clear()

                val pages = requestTwoSingleItemPages(field = "CREATED_AT", searchKeyword = "동시등록")

                assertThat(responseTitles(pages.first)).containsExactly("동시등록 둘째 공고")
                assertThat(responseTitles(pages.second)).containsExactly("동시등록 첫 공고")
            }

            @Test
            fun `SALARY 커서는 급여가 같아도 id 역순으로 이어서 조회한다`() {
                listOf("동일급여 첫 공고", "동일급여 둘째 공고").forEach { title ->
                    entityManager.persist(TestFixtures.jobPosting(title = title, salaryMin = 70_000_000))
                }
                entityManager.flush()
                entityManager.clear()

                val pages = requestTwoSingleItemPages(field = "SALARY", searchKeyword = "동일급여")

                assertThat(responseTitles(pages.first)).containsExactly("동일급여 둘째 공고")
                assertThat(responseTitles(pages.second)).containsExactly("동일급여 첫 공고")
            }

            @Test
            fun `SALARY는 급여 미공개 공고를 제거하지 않고 마지막에 배치한다`() {
                entityManager.persist(
                    TestFixtures.jobPosting(title = "급여보존 공개 공고", salaryMin = 40_000_000)
                )
                entityManager.persist(
                    TestFixtures.jobPosting(title = "급여보존 미공개 공고", salaryMin = null)
                )
                entityManager.flush()
                entityManager.clear()

                val pages = requestTwoSingleItemPages(field = "SALARY", searchKeyword = "급여보존")

                assertThat(responseTitles(pages.first)).containsExactly("급여보존 공개 공고")
                assertThat(responseTitles(pages.second)).containsExactly("급여보존 미공개 공고")
            }

            @Test
            fun `기존 id 전용 커서는 기본 정렬에서 이어서 조회한다`() {
                val firstPage = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("field", "CREATED_AT")
                        .param("size", "1")
                )
                    .andExpect(status().isOk)
                    .andReturn()
                val firstBody = objectMapper.readTree(firstPage.response.contentAsString)
                val firstId = firstBody.get("content").first().get("id").asLong()
                val legacyCursor = Base64.getUrlEncoder().encodeToString(
                    "{\"id\":$firstId}".toByteArray(Charsets.UTF_8)
                )

                val secondPage = mockMvc.perform(
                    get("/api/v1/job-postings")
                        .param("field", "CREATED_AT")
                        .param("size", "1")
                        .param("cursor", legacyCursor)
                )
                    .andExpect(status().isOk)
                    .andReturn()

                assertThat(responseTitles(secondPage.response.contentAsString))
                    .containsExactly("대전 정규직 간호사")
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

    private fun responseTitles(body: String): List<String> = objectMapper.readTree(body)
        .get("content")
        .map { it.get("wantedTitle").asText() }

    private fun requestTwoSingleItemPages(field: String, searchKeyword: String): Pair<String, String> {
        val firstPage = mockMvc.perform(
            get("/api/v1/job-postings")
                .param("field", field)
                .param("searchKeyword", searchKeyword)
                .param("size", "1")
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        val cursor = objectMapper.readTree(firstPage).get("pageInfo").get("nextCursor").asText()
        val secondPage = mockMvc.perform(
            get("/api/v1/job-postings")
                .param("field", field)
                .param("searchKeyword", searchKeyword)
                .param("size", "1")
                .param("cursor", cursor)
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return firstPage to secondPage
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
