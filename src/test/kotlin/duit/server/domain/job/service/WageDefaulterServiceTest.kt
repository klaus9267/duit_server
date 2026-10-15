package duit.server.domain.job.service

import duit.server.domain.job.entity.Company
import duit.server.domain.job.repository.JobCompanyRepository
import duit.server.infrastructure.external.job.work24.WageDefaulterCheckResult
import duit.server.infrastructure.external.job.work24.Work24WageDefaulterClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("WageDefaulterService 단위 테스트")
class WageDefaulterServiceTest {

    private lateinit var repository: JobCompanyRepository
    private lateinit var client: Work24WageDefaulterClient
    private lateinit var service: WageDefaulterService
    private val now = LocalDateTime.of(2026, 10, 15, 12, 0)

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        client = mockk()
        every { client.isEnabled } returns true
        service = WageDefaulterService(repository, client, requestIntervalMs = 0)
    }

    private fun company(id: Long, businessNumber: String?) = Company(id = id, businessNumber = businessNumber)

    @Test
    fun `API 키가 없으면 조회하지 않는다`() {
        every { client.isEnabled } returns false

        service.refreshStaleCompanies(now)

        verify(exactly = 0) { repository.findWageDefaulterCheckTargets(any()) }
    }

    @Test
    fun `24시간 이전에 확인한 회사까지 대상으로 조회한다`() {
        every { repository.findWageDefaulterCheckTargets(any()) } returns emptyList()

        service.refreshStaleCompanies(now)

        verify(exactly = 1) { repository.findWageDefaulterCheckTargets(now.minusHours(24)) }
    }

    @Test
    fun `명단공개 여부를 사업자등록번호 숫자만으로 조회해 저장한다`() {
        every { repository.findWageDefaulterCheckTargets(any()) } returns listOf(
            company(1L, "123-45-67890"),
            company(2L, "2234567890"),
        )
        every { client.check("1234567890") } returns WageDefaulterCheckResult.Checked(listed = true)
        every { client.check("2234567890") } returns WageDefaulterCheckResult.Checked(listed = false)

        service.refreshStaleCompanies(now)

        verify(exactly = 1) { repository.updateWageDefaulter(1L, true, any()) }
        verify(exactly = 1) { repository.updateWageDefaulter(2L, false, any()) }
    }

    @Test
    fun `판정할 수 없으면 기존 값을 유지한다`() {
        every { repository.findWageDefaulterCheckTargets(any()) } returns listOf(company(1L, "1234567890"))
        every { client.check("1234567890") } returns WageDefaulterCheckResult.Unknown("연계 실패")

        service.refreshStaleCompanies(now)

        verify(exactly = 0) { repository.updateWageDefaulter(any(), any(), any()) }
    }

    @Test
    fun `API 키 오류가 나면 남은 회사는 조회하지 않는다`() {
        every { repository.findWageDefaulterCheckTargets(any()) } returns listOf(
            company(1L, "1234567890"),
            company(2L, "2234567890"),
        )
        every { client.check("1234567890") } returns WageDefaulterCheckResult.Unknown("오류", errorCode = "ERR0001")

        service.refreshStaleCompanies(now)

        verify(exactly = 0) { client.check("2234567890") }
    }

    @Test
    fun `API 장애가 연속되면 남은 회사는 다음 실행으로 미룬다`() {
        every { repository.findWageDefaulterCheckTargets(any()) } returns (1L..5L).map {
            company(it, (1_000_000_000L + it).toString())
        }
        every { client.check(any()) } returns WageDefaulterCheckResult.Unknown("요청 실패", apiFailure = true)

        service.refreshStaleCompanies(now)

        verify(exactly = WageDefaulterService.MAX_CONSECUTIVE_API_FAILURES) { client.check(any()) }
    }

    @Test
    fun `회사별 판정 불가나 중간 성공은 연속 장애로 세지 않는다`() {
        every { repository.findWageDefaulterCheckTargets(any()) } returns (1L..6L).map {
            company(it, (1_000_000_000L + it).toString())
        }
        val apiFailure = WageDefaulterCheckResult.Unknown("요청 실패", apiFailure = true)
        every { client.check(any()) } returnsMany listOf(
            apiFailure,
            apiFailure,
            WageDefaulterCheckResult.Unknown("연계 실패: lnkSucsYn=N"),
            apiFailure,
            apiFailure,
            WageDefaulterCheckResult.Checked(listed = false),
        )

        service.refreshStaleCompanies(now)

        verify(exactly = 6) { client.check(any()) }
        verify(exactly = 1) { repository.updateWageDefaulter(6L, false, any()) }
    }

    @Test
    fun `사업자등록번호 형식이 맞지 않는 회사는 건너뛴다`() {
        every { repository.findWageDefaulterCheckTargets(any()) } returns listOf(
            company(1L, "12-34"),
            company(2L, "test-123"),
            company(3L, "1234567890"),
        )
        every { client.check(any()) } returns WageDefaulterCheckResult.Checked(listed = false)

        service.refreshStaleCompanies(now)

        verify(exactly = 1) { client.check(any()) }
        verify(exactly = 1) { repository.updateWageDefaulter(3L, false, any()) }
    }

    @Test
    fun `1회 실행당 조회 수를 제한한다`() {
        every { repository.findWageDefaulterCheckTargets(any()) } returns (1L..350L).map {
            company(it, (1_000_000_000L + it).toString())
        }
        every { client.check(any()) } returns WageDefaulterCheckResult.Checked(listed = false)

        service.refreshStaleCompanies(now)

        verify(exactly = WageDefaulterService.MAX_CHECKS_PER_RUN) { client.check(any()) }
    }
}
