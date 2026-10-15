package duit.server.application.scheduler

import duit.server.domain.job.service.JobSyncService
import duit.server.domain.job.service.WageDefaulterService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JobSyncScheduler 단위 테스트")
class JobSyncSchedulerTest {

    private lateinit var jobSyncService: JobSyncService
    private lateinit var wageDefaulterService: WageDefaulterService
    private lateinit var scheduler: JobSyncScheduler

    @BeforeEach
    fun setUp() {
        jobSyncService = mockk()
        wageDefaulterService = mockk()
        justRun { wageDefaulterService.refreshStaleCompanies(any()) }
        scheduler = JobSyncScheduler(jobSyncService, wageDefaulterService)
    }

    @Nested
    @DisplayName("syncJobs()")
    inner class SyncJobsTests {

        @Test
        fun `syncJobs 호출 시 jobSyncService syncAll이 실행된다`() {
            justRun { jobSyncService.syncAll() }

            scheduler.syncJobs()

            verify(exactly = 1) { jobSyncService.syncAll() }
        }

        @Test
        fun `syncAll에서 예외 발생 시 예외를 다시 던지지 않는다`() {
            every { jobSyncService.syncAll() } throws RuntimeException("수집 오류")

            scheduler.syncJobs()

            verify(exactly = 1) { jobSyncService.syncAll() }
        }

        @Test
        fun `공고 수집 후 임금체불 명단 여부를 갱신한다`() {
            justRun { jobSyncService.syncAll() }

            scheduler.syncJobs()

            verifyOrder {
                jobSyncService.syncAll()
                wageDefaulterService.refreshStaleCompanies(any())
            }
        }

        @Test
        fun `임금체불 명단 갱신이 실패해도 예외를 다시 던지지 않는다`() {
            justRun { jobSyncService.syncAll() }
            every { wageDefaulterService.refreshStaleCompanies(any()) } throws RuntimeException("API 오류")

            scheduler.syncJobs()

            verify(exactly = 1) { wageDefaulterService.refreshStaleCompanies(any()) }
        }
    }
}
