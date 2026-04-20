package duit.server.domain.job.service

import duit.server.domain.job.entity.JobCompany
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.JobSyncState
import duit.server.domain.job.entity.SourceType
import duit.server.domain.job.repository.JobCompanyRepository
import duit.server.domain.job.repository.JobPostingRepository
import duit.server.domain.job.repository.JobSyncStateRepository
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.job.JobFetcher
import duit.server.infrastructure.external.job.dto.IncrementalFetchResult
import duit.server.infrastructure.external.job.dto.JobFetchResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("JobSyncService 단위 테스트")
class JobSyncServiceTest {

    private lateinit var fetcher: JobFetcher
    private lateinit var companyRepository: JobCompanyRepository
    private lateinit var repository: JobPostingRepository
    private lateinit var syncStateRepository: JobSyncStateRepository
    private lateinit var discordService: DiscordService
    private lateinit var syncService: JobSyncService

    @BeforeEach
    fun setUp() {
        fetcher = mockk()
        companyRepository = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        syncStateRepository = mockk(relaxed = true)
        discordService = mockk(relaxed = true)

        every { fetcher.sourceType } returns SourceType.WORK24
        every { companyRepository.findByBusinessNumber(any()) } returns null
        every { companyRepository.findByCorpNm(any()) } returns null
        every { companyRepository.save(any<JobCompany>()) } answers { firstArg() }
        every { syncStateRepository.findById(any<SourceType>()) } returns Optional.empty()
        every { syncStateRepository.save(any<JobSyncState>()) } answers { firstArg() }

        syncService = JobSyncService(listOf(fetcher), companyRepository, repository, syncStateRepository, discordService)
    }

    private fun createFetchResult(
        externalId: String = "K123456",
        title: String = "간호사 채용",
        companyName: String = "테스트병원",
        businessNumber: String? = null,
        isActive: Boolean = true,
    ) = JobFetchResult(
        externalId = externalId,
        title = title,
        companyName = companyName,
        businessNumber = businessNumber,
        jobCategory = "304000",
        location = "서울특별시 강남구",
        zipCode = null,
        roadNameAddress = null,
        basicAddress = null,
        detailAddress = null,
        infoService = null,
        workRegion = null,
        workDistrict = null,
        employmentType = null,
        careerMin = null,
        careerMax = null,
        educationLevel = null,
        salaryMin = null,
        salaryMax = null,
        salaryType = null,
        postingUrl = "https://example.com/jobs/$externalId",
        postedAt = LocalDateTime.now(),
        expiresAt = LocalDateTime.now().plusDays(7),
        closeType = duit.server.domain.job.entity.CloseType.FIXED,
        isActive = isActive,
        workHoursPerWeek = null,
    )

    private fun createJobPosting(
        id: Long? = 1L,
        wantedAuthNo: String = "K123456",
        title: String = "기존 제목",
        companyName: String = "기존병원",
        isActive: Boolean = true,
    ) = JobPosting(
        id = id,
        wantedAuthNo = wantedAuthNo,
        isActive = isActive,
    ).apply {
        updateWork24Detail(
            detail = duit.server.domain.job.entity.JobPostingWork24Detail(
                wantedTitle = title,
            ),
            company = JobCompany(corpNm = companyName),
        )
    }

    @Nested
    @DisplayName("syncAll()")
    inner class SyncAllTests {

        @Test
        fun `신규 공고는 wantedAuthNo 기준으로 저장된다`() {
            every { fetcher.fetchAll() } returns listOf(createFetchResult())
            every { repository.findByWantedAuthNo("K123456") } returns null
            every { repository.save(any()) } answers { firstArg() }

            val saved = slot<JobPosting>()
            every { repository.save(capture(saved)) } answers { saved.captured }

            syncService.syncAll()

            verify(exactly = 1) { repository.save(any()) }
            assertEquals("K123456", saved.captured.wantedAuthNo)
            assertEquals("간호사 채용", saved.captured.wantedTitle)
            assertEquals("테스트병원", saved.captured.company?.corpNm)
        }

        @Test
        fun `사업자등록번호가 있으면 회사는 businessNumber 기준으로 조회한다`() {
            val existingCompany = JobCompany(
                businessNumber = "123-45-67890",
                corpNm = "기존병원",
            )

            every { fetcher.fetchAll() } returns listOf(
                createFetchResult(
                    companyName = "변경된병원명",
                    businessNumber = "123-45-67890",
                )
            )
            every { repository.findByWantedAuthNo("K123456") } returns null
            every { companyRepository.findByBusinessNumber("123-45-67890") } returns existingCompany
            every { repository.save(any()) } answers { firstArg() }

            syncService.syncAll()

            verify(exactly = 1) { companyRepository.findByBusinessNumber("123-45-67890") }
            verify(exactly = 0) { companyRepository.findByCorpNm("변경된병원명") }
            assertEquals("변경된병원명", existingCompany.corpNm)
        }

        @Test
        fun `기존 공고는 wantedAuthNo 기준으로 상세를 갱신한다`() {
            val existing = createJobPosting()
            every { fetcher.fetchAll() } returns listOf(
                createFetchResult(
                    externalId = "K123456",
                    title = "변경된 제목",
                    companyName = "새병원",
                    isActive = false,
                )
            )
            every { repository.findByWantedAuthNo("K123456") } returns existing

            syncService.syncAll()

            verify(exactly = 0) { repository.save(any()) }
            assertEquals("변경된 제목", existing.wantedTitle)
            assertEquals("새병원", existing.company?.corpNm)
            assertEquals(false, existing.isActive)
        }
    }

    @Nested
    @DisplayName("syncIncremental()")
    inner class SyncIncrementalTests {

        @Test
        fun `워터마크가 없으면 fetchAll로 fallback 한다`() {
            every { syncStateRepository.findAll() } returns emptyList()
            every { fetcher.fetchAll() } returns listOf(createFetchResult(externalId = "K999999"))
            every { repository.findByWantedAuthNo("K999999") } returns null
            every { repository.save(any()) } answers { firstArg() }

            syncService.syncIncremental()

            verify(exactly = 1) { fetcher.fetchAll() }
            verify(exactly = 0) { fetcher.fetchIncremental(any()) }
        }

        @Test
        fun `워터마크가 있으면 fetchIncremental을 호출한다`() {
            val lastSynced = LocalDateTime.of(2025, 3, 15, 12, 0)
            every { syncStateRepository.findAll() } returns listOf(
                JobSyncState(SourceType.WORK24, lastSyncedAt = lastSynced)
            )
            every { fetcher.fetchIncremental(any()) } returns IncrementalFetchResult(
                items = emptyList(),
                latestTimestamp = LocalDateTime.of(2025, 3, 15, 14, 0),
            )

            syncService.syncIncremental()

            verify(exactly = 1) { fetcher.fetchIncremental(lastSynced.minusMinutes(1)) }
        }
    }
}
