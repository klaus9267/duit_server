package duit.server.application.scheduler

import duit.server.domain.job.service.JobSyncService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JobSyncScheduler 단위 테스트")
class JobSyncSchedulerTest {

    private lateinit var jobSyncService: JobSyncService
    private lateinit var scheduler: JobSyncScheduler

    @BeforeEach
    fun setUp() {
        jobSyncService = mockk()
        scheduler = JobSyncScheduler(jobSyncService)
    }

    @Nested
    @DisplayName("syncAllJobs()")
    inner class SyncAllJobsTests {

        @Test
        fun `syncAllJobs 호출 시 jobSyncService syncAll이 실행된다`() {
            justRun { jobSyncService.syncAll() }

            scheduler.syncAllJobs()

            verify(exactly = 1) { jobSyncService.syncAll() }
        }

        @Test
        fun `syncAll에서 예외 발생 시 예외를 다시 던지지 않는다`() {
            every { jobSyncService.syncAll() } throws RuntimeException("동기화 오류")

            scheduler.syncAllJobs()

            verify(exactly = 1) { jobSyncService.syncAll() }
        }
    }

    @Nested
    @DisplayName("syncIncrementalJobs()")
    inner class SyncIncrementalJobsTests {

        @Test
        fun `syncIncrementalJobs 호출 시 jobSyncService syncIncremental이 실행된다`() {
            justRun { jobSyncService.syncIncremental() }

            scheduler.syncIncrementalJobs()

            verify(exactly = 1) { jobSyncService.syncIncremental() }
        }

        @Test
        fun `syncIncremental에서 예외 발생 시 예외를 다시 던지지 않는다`() {
            every { jobSyncService.syncIncremental() } throws RuntimeException("증분 동기화 오류")

            scheduler.syncIncrementalJobs()

            verify(exactly = 1) { jobSyncService.syncIncremental() }
        }
    }
}
