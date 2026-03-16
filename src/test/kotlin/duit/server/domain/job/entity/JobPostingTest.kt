package duit.server.domain.job.entity

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

@DisplayName("JobPosting 엔티티 단위 테스트")
class JobPostingTest {

    private fun createJobPosting(
        sourceType: SourceType = SourceType.WORK24,
        externalId: String = "K123",
        title: String = "간호사 채용",
        companyName: String = "테스트병원",
        jobCategory: String? = "3040",
        location: String? = "서울특별시 강남구",
        workRegion: WorkRegion? = WorkRegion.SEOUL,
        workDistrict: String? = "강남구",
        employmentType: EmploymentType? = EmploymentType.FULL_TIME,
        careerMin: Int? = null,
        careerMax: Int? = null,
        educationLevel: EducationLevel? = null,
        salaryMin: Long? = null,
        salaryMax: Long? = null,
        salaryType: SalaryType? = null,
        postingUrl: String = "https://example.com",
        postedAt: LocalDateTime? = LocalDateTime.now().minusDays(1),
        expiresAt: LocalDateTime? = LocalDateTime.now().plusDays(7),
        closeType: CloseType = CloseType.FIXED,
        isActive: Boolean = true,
        workHoursPerWeek: Int? = null,
    ) = JobPosting(
        sourceType = sourceType,
        externalId = externalId,
        title = title,
        companyName = companyName,
        jobCategory = jobCategory,
        location = location,
        workRegion = workRegion,
        workDistrict = workDistrict,
        employmentType = employmentType,
        careerMin = careerMin,
        careerMax = careerMax,
        educationLevel = educationLevel,
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        salaryType = salaryType,
        postingUrl = postingUrl,
        postedAt = postedAt,
        expiresAt = expiresAt,
        closeType = closeType,
        isActive = isActive,
        workHoursPerWeek = workHoursPerWeek,
    )

    @Nested
    @DisplayName("updateFromSource()")
    inner class UpdateFromSourceTests {

        @Test
        fun `모든 필드가 업데이트된다`() {
            val posting = createJobPosting(
                title = "기존 제목",
                companyName = "기존 병원",
                jobCategory = "3040",
                location = "서울",
                workRegion = WorkRegion.SEOUL,
                workDistrict = "강남구",
                employmentType = EmploymentType.FULL_TIME,
                salaryMin = 10000L,
                salaryMax = 20000L,
                salaryType = SalaryType.ANNUAL,
                postingUrl = "https://old.com",
                closeType = CloseType.FIXED,
                isActive = true,
            )

            posting.updateFromSource(
                title = "새 제목",
                companyName = "새 병원",
                jobCategory = "3040001",
                location = "부산",
                workRegion = WorkRegion.BUSAN,
                workDistrict = "해운대구",
                employmentType = EmploymentType.CONTRACT,
                careerMin = 1,
                careerMax = 5,
                educationLevel = EducationLevel.BACHELOR,
                salaryMin = 30000L,
                salaryMax = 50000L,
                salaryType = SalaryType.MONTHLY,
                postingUrl = "https://new.com",
                postedAt = LocalDateTime.of(2025, 1, 1, 0, 0),
                expiresAt = LocalDateTime.of(2025, 12, 31, 0, 0),
                closeType = CloseType.ON_HIRE,
                isActive = false,
                workHoursPerWeek = 40,
            )

            assertEquals("새 제목", posting.title)
            assertEquals("새 병원", posting.companyName)
            assertEquals("3040001", posting.jobCategory)
            assertEquals("부산", posting.location)
            assertEquals(WorkRegion.BUSAN, posting.workRegion)
            assertEquals("해운대구", posting.workDistrict)
            assertEquals(EmploymentType.CONTRACT, posting.employmentType)
            assertEquals(1, posting.careerMin)
            assertEquals(5, posting.careerMax)
            assertEquals(EducationLevel.BACHELOR, posting.educationLevel)
            assertEquals(30000L, posting.salaryMin)
            assertEquals(50000L, posting.salaryMax)
            assertEquals(SalaryType.MONTHLY, posting.salaryType)
            assertEquals("https://new.com", posting.postingUrl)
            assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), posting.postedAt)
            assertEquals(LocalDateTime.of(2025, 12, 31, 0, 0), posting.expiresAt)
            assertEquals(CloseType.ON_HIRE, posting.closeType)
            assertFalse(posting.isActive)
            assertEquals(40, posting.workHoursPerWeek)
        }

        @Test
        fun `nullable 필드를 null로 업데이트 가능`() {
            val posting = createJobPosting(
                jobCategory = "3040",
                location = "서울",
                workRegion = WorkRegion.SEOUL,
                salaryMin = 10000L,
            )

            posting.updateFromSource(
                title = "제목",
                companyName = "병원",
                jobCategory = null,
                location = null,
                workRegion = null,
                workDistrict = null,
                employmentType = null,
                careerMin = null,
                careerMax = null,
                educationLevel = null,
                salaryMin = null,
                salaryMax = null,
                salaryType = null,
                postingUrl = "https://example.com",
                postedAt = null,
                expiresAt = null,
                closeType = CloseType.ON_HIRE,
                isActive = true,
                workHoursPerWeek = null,
            )

            assertEquals(null, posting.jobCategory)
            assertEquals(null, posting.location)
            assertEquals(null, posting.workRegion)
            assertEquals(null, posting.salaryMin)
        }
    }

    @Nested
    @DisplayName("syncActiveStatus()")
    inner class SyncActiveStatusTests {

        @Test
        fun `FIXED 공고 — 만료 전이면 isActive true 유지`() {
            val posting = createJobPosting(
                closeType = CloseType.FIXED,
                expiresAt = LocalDateTime.now().plusDays(1),
                isActive = true,
            )

            posting.syncActiveStatus(LocalDateTime.now())

            assertTrue(posting.isActive)
        }

        @Test
        fun `FIXED 공고 — 만료 후이면 isActive false로 변경`() {
            val posting = createJobPosting(
                closeType = CloseType.FIXED,
                expiresAt = LocalDateTime.now().minusDays(1),
                isActive = true,
            )

            posting.syncActiveStatus(LocalDateTime.now())

            assertFalse(posting.isActive)
        }

        @Test
        fun `FIXED 공고 — expiresAt이 null이면 isActive true 유지`() {
            val posting = createJobPosting(
                closeType = CloseType.FIXED,
                expiresAt = null,
                isActive = true,
            )

            posting.syncActiveStatus(LocalDateTime.now())

            assertTrue(posting.isActive)
        }

        @Test
        fun `ON_HIRE 공고는 syncActiveStatus 호출해도 변경 없음`() {
            val posting = createJobPosting(
                closeType = CloseType.ON_HIRE,
                expiresAt = LocalDateTime.now().minusDays(1),
                isActive = true,
            )

            posting.syncActiveStatus(LocalDateTime.now())

            assertTrue(posting.isActive)
        }

        @Test
        fun `FIXED 공고 — expiresAt이 정확히 now와 같으면 isActive false`() {
            val now = LocalDateTime.of(2025, 3, 15, 12, 0)
            val posting = createJobPosting(
                closeType = CloseType.FIXED,
                expiresAt = now,
                isActive = true,
            )

            posting.syncActiveStatus(now)

            assertFalse(posting.isActive)
        }
    }

    @Nested
    @DisplayName("careerDescription")
    inner class CareerDescriptionTests {

        @Test
        fun `경력 무관 — min과 max 모두 null`() {
            val posting = createJobPosting(careerMin = null, careerMax = null)
            assertEquals("경력무관", posting.careerDescription)
        }

        @Test
        fun `신입 — min이 0이고 max가 null`() {
            val posting = createJobPosting(careerMin = 0, careerMax = null)
            assertEquals("신입", posting.careerDescription)
        }

        @Test
        fun `경력 N년 이상 — min만 있고 max null`() {
            val posting = createJobPosting(careerMin = 3, careerMax = null)
            assertEquals("경력 3년 이상", posting.careerDescription)
        }

        @Test
        fun `경력 N~M년 — min과 max 모두 있음`() {
            val posting = createJobPosting(careerMin = 1, careerMax = 5)
            assertEquals("경력 1~5년", posting.careerDescription)
        }

        @Test
        fun `경력 범위 — min이 null이고 max만 있으면 경력무관으로 처리`() {
            val posting = createJobPosting(careerMin = null, careerMax = 3)
            assertEquals("경력무관", posting.careerDescription)
        }
    }

    @Nested
    @DisplayName("salaryDescription")
    inner class SalaryDescriptionTests {

        @Test
        fun `급여 미공개 — salaryMin이 null`() {
            val posting = createJobPosting(salaryMin = null, salaryMax = null, salaryType = null)
            assertEquals("급여 미공개", posting.salaryDescription)
        }

        @Test
        fun `연봉 범위 표시`() {
            val posting = createJobPosting(
                salaryMin = 30000000L,
                salaryMax = 40000000L,
                salaryType = SalaryType.ANNUAL,
            )
            assertEquals("연봉 3000~4000만원", posting.salaryDescription)
        }

        @Test
        fun `연봉 단일값 — min과 max가 같으면 범위 표시 안 함`() {
            val posting = createJobPosting(
                salaryMin = 30000000L,
                salaryMax = 30000000L,
                salaryType = SalaryType.ANNUAL,
            )
            assertEquals("연봉 3000만원", posting.salaryDescription)
        }

        @Test
        fun `월급 표시`() {
            val posting = createJobPosting(
                salaryMin = 2500000L,
                salaryMax = 3000000L,
                salaryType = SalaryType.MONTHLY,
            )
            assertEquals("월급 250~300만원", posting.salaryDescription)
        }

        @Test
        fun `salaryType이 null이면 타입명 생략`() {
            val posting = createJobPosting(
                salaryMin = 30000000L,
                salaryMax = null,
                salaryType = null,
            )
            assertEquals("3000만원", posting.salaryDescription)
        }

        @Test
        fun `salaryMax가 0이면 범위 표시 안 함`() {
            val posting = createJobPosting(
                salaryMin = 30000000L,
                salaryMax = 0L,
                salaryType = SalaryType.ANNUAL,
            )
            assertEquals("연봉 3000만원", posting.salaryDescription)
        }

        @Test
        fun `salaryMax가 null이면 단일값 표시`() {
            val posting = createJobPosting(
                salaryMin = 30000000L,
                salaryMax = null,
                salaryType = SalaryType.ANNUAL,
            )
            assertEquals("연봉 3000만원", posting.salaryDescription)
        }
    }
}
