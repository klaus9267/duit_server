package duit.server.application.scheduler

import duit.server.domain.job.service.JobSyncService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
@Profile("prod")
class JobSyncScheduler(
    private val jobSyncService: JobSyncService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** 매 3시간마다 증분 수집 (경량) */
    @Scheduled(cron = "0 0 */3 * * *")
    fun syncIncrementalJobs() {
        logger.info("=== Starting incremental job sync ===")
        try {
            jobSyncService.syncIncremental()
            logger.info("=== Incremental job sync completed successfully ===")
        } catch (e: Exception) {
            logger.error("Error during incremental job sync", e)
        }
    }
}
