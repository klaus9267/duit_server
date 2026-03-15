package duit.server.domain.job.service

import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.JobSyncState
import duit.server.domain.job.entity.SourceType
import duit.server.domain.job.repository.JobPostingRepository
import duit.server.domain.job.repository.JobSyncStateRepository
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.job.JobFetcher
import duit.server.infrastructure.external.job.dto.JobFetchResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import java.util.Optional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("JobSyncService 단위 테스트")
class JobSyncServiceTest {

    private lateinit var fetcher1: JobFetcher
    private lateinit var repository: JobPostingRepository
    private lateinit var syncStateRepository: JobSyncStateRepository
    private lateinit var discordService: DiscordService
    private lateinit var syncService: JobSyncService

    @BeforeEach
    fun setUp() {
        fetcher1 = mockk()
        repository = mockk(relaxed = true)
        syncStateRepository = mockk(relaxed = true)
        discordService = mockk(relaxed = true)
        every { fetcher1.sourceType } returns SourceType.SARAMIN
        every { syncStateRepository.findById(any<SourceType>()) } returns Optional.empty()
        every { syncStateRepository.save(any<JobSyncState>()) } answers { firstArg() }
        syncService = JobSyncService(listOf(fetcher1), repository, syncStateRepository, discordService)
    }

    private fun createFetchResult(
        externalId: String = "ext-1",
        title: String = "간호사 채용",
        companyName: String = "테스트병원",
        jobCategory: String? = "간호사",
        location: String? = "서울특별시 강남구",
        workRegion: duit.server.domain.job.entity.WorkRegion? = null,
        workDistrict: String? = "강남구",
        employmentType: duit.server.domain.job.entity.EmploymentType? = null,
        careerMin: Int? = null,
        careerMax: Int? = null,
        educationLevel: duit.server.domain.job.entity.EducationLevel? = null,
        salaryMin: Long? = null,
        salaryMax: Long? = null,
        salaryType: duit.server.domain.job.entity.SalaryType? = null,
        postingUrl: String = "https://example.com/job/1",
        postedAt: LocalDateTime? = LocalDateTime.now().minusDays(1),
        expiresAt: LocalDateTime? = LocalDateTime.now().plusDays(7),
        closeType: CloseType = CloseType.FIXED,
        isActive: Boolean = true,
        workHoursPerWeek: Int? = null,
    ): JobFetchResult = JobFetchResult(
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

    private fun createJobPosting(
        id: Long? = 1L,
        sourceType: SourceType = SourceType.SARAMIN,
        externalId: String = "ext-1",
        title: String = "기존 공고",
        companyName: String = "기존병원",
        jobCategory: String? = "간호사",
        location: String? = "서울특별시 강남구",
        workRegion: duit.server.domain.job.entity.WorkRegion? = null,
        workDistrict: String? = "강남구",
        employmentType: duit.server.domain.job.entity.EmploymentType? = null,
        careerMin: Int? = null,
        careerMax: Int? = null,
        educationLevel: duit.server.domain.job.entity.EducationLevel? = null,
        salaryMin: Long? = null,
        salaryMax: Long? = null,
        salaryType: duit.server.domain.job.entity.SalaryType? = null,
        postingUrl: String = "https://example.com/job/1",
        postedAt: LocalDateTime? = LocalDateTime.now().minusDays(2),
        expiresAt: LocalDateTime? = LocalDateTime.now().plusDays(5),
        closeType: CloseType = CloseType.FIXED,
        isActive: Boolean = true,
        workHoursPerWeek: Int? = null,
    ): JobPosting = JobPosting(
        id = id,
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
    @DisplayName("fetchAllAsync 비동기 동작")
    inner class FetchAllAsyncTests {

        @Test
        fun `두 fetcher가 병렬로 모두 호출된다`() {
            val fetcher2 = mockk<JobFetcher>()
            every { fetcher2.sourceType } returns SourceType.WORK24

            val syncServiceWith2Fetchers = JobSyncService(listOf(fetcher1, fetcher2), repository, syncStateRepository, discordService)

            every { fetcher1.fetchAll() } returns listOf(createFetchResult(externalId = "saramin-1"))
            every { fetcher2.fetchAll() } returns listOf(createFetchResult(externalId = "work24-1"))
            every { repository.findBySourceTypeAndExternalId(SourceType.SARAMIN, "saramin-1") } returns null
            every { repository.findBySourceTypeAndExternalId(SourceType.WORK24, "work24-1") } returns null
            every { repository.findByIsActiveTrueAndExpiresAtBefore(any()) } returns emptyList()
            every { repository.save(any()) } returns mockk()

            syncServiceWith2Fetchers.syncAll()

            verify(exactly = 1) { fetcher1.fetchAll() }
            verify(exactly = 1) { fetcher2.fetchAll() }
            verify(exactly = 2) { repository.save(any()) }
        }

        @Test
        fun `한 fetcher가 실패해도 다른 fetcher 결과는 정상 처리된다`() {
            val fetcher2 = mockk<JobFetcher>()
            every { fetcher2.sourceType } returns SourceType.WORK24

            val syncServiceWith2Fetchers = JobSyncService(listOf(fetcher1, fetcher2), repository, syncStateRepository, discordService)

            every { fetcher1.fetchAll() } throws RuntimeException("fetcher1 오류")
            every { fetcher2.fetchAll() } returns listOf(createFetchResult(externalId = "work24-ok"))
            every { repository.findBySourceTypeAndExternalId(SourceType.WORK24, "work24-ok") } returns null
            every { repository.findByIsActiveTrueAndExpiresAtBefore(any()) } returns emptyList()
            every { repository.save(any()) } returns mockk()

            syncServiceWith2Fetchers.syncAll()

            verify(exactly = 0) { repository.findBySourceTypeAndExternalId(SourceType.SARAMIN, any()) }
            verify(exactly = 1) { repository.save(any()) }
        }
    }

    @Nested
    inner class SyncAllTests {

        @Test
        fun `신규 공고는 repository save가 호출된다`() {
            val fetchResult = createFetchResult(
                externalId = "ext-new",
                title = "신규 간호사 채용",
                companyName = "신규병원",
            )
            every { fetcher1.fetchAll() } returns listOf(fetchResult)
            every { repository.findBySourceTypeAndExternalId(SourceType.SARAMIN, "ext-new") } returns null
            every { repository.findByIsActiveTrueAndExpiresAtBefore(any()) } returns emptyList()

            val savedSlot = slot<JobPosting>()
            every { repository.save(capture(savedSlot)) } returns mockk()

            syncService.syncAll()

            verify(exactly = 1) { repository.save(any()) }
            assertEquals("신규 간호사 채용", savedSlot.captured.title)
            assertEquals("신규병원", savedSlot.captured.companyName)
            assertEquals(SourceType.SARAMIN, savedSlot.captured.sourceType)
            assertEquals("ext-new", savedSlot.captured.externalId)
        }

        @Test
        fun `기존 공고는 save 없이 엔티티가 업데이트된다`() {
            val fetchResult = createFetchResult(
                externalId = "ext-1",
                title = "업데이트된 공고 제목",
                companyName = "업데이트병원",
            )
            val existingPosting = createJobPosting(
                externalId = "ext-1",
                title = "기존 공고",
                companyName = "기존병원",
            )
            every { fetcher1.fetchAll() } returns listOf(fetchResult)
            every { repository.findBySourceTypeAndExternalId(SourceType.SARAMIN, "ext-1") } returns existingPosting
            every { repository.findByIsActiveTrueAndExpiresAtBefore(any()) } returns emptyList()

            syncService.syncAll()

            verify(exactly = 0) { repository.save(any()) }
            assertEquals("업데이트된 공고 제목", existingPosting.title)
        }

        @Test
        fun `만료된 FIXED 공고는 isActive가 false로 변경된다`() {
            every { fetcher1.fetchAll() } returns emptyList()

            val expiredPosting = createJobPosting(
                externalId = "ext-expired",
                closeType = CloseType.FIXED,
                expiresAt = LocalDateTime.now().minusDays(1),
                isActive = true,
            )
            every { repository.findByIsActiveTrueAndExpiresAtBefore(any()) } returns listOf(expiredPosting)

            syncService.syncAll()

            assertFalse(expiredPosting.isActive)
        }

        @Test
        fun `첫 번째 fetcher 예외 시 두 번째 fetcher 결과는 정상 저장된다`() {
            val fetcher2 = mockk<JobFetcher>()
            every { fetcher2.sourceType } returns SourceType.WORK24

            val syncServiceWith2Fetchers = JobSyncService(listOf(fetcher1, fetcher2), repository, syncStateRepository, discordService)

            every { fetcher1.fetchAll() } throws RuntimeException("사람인 API 오류")
            val fetchResult = createFetchResult(
                externalId = "ext-work24",
                title = "고용24 공고",
                companyName = "고용24병원",
            )
            every { fetcher2.fetchAll() } returns listOf(fetchResult)
            every { repository.findBySourceTypeAndExternalId(SourceType.WORK24, "ext-work24") } returns null
            every { repository.findByIsActiveTrueAndExpiresAtBefore(any()) } returns emptyList()

            val savedSlot = slot<JobPosting>()
            every { repository.save(capture(savedSlot)) } returns mockk()

            syncServiceWith2Fetchers.syncAll()

            verify(exactly = 1) { repository.save(any()) }
            assertEquals("고용24 공고", savedSlot.captured.title)
            assertEquals("고용24병원", savedSlot.captured.companyName)
            assertEquals(SourceType.WORK24, savedSlot.captured.sourceType)
            assertEquals("ext-work24", savedSlot.captured.externalId)
        }
    }
}
