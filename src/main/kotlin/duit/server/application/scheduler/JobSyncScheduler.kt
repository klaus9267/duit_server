package duit.server.application.scheduler

import duit.server.domain.job.service.JobSyncService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
//@Profile("prod")
class JobSyncScheduler(
    private val jobSyncService: JobSyncService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        syncJobs()
    }

    /** 매 3시간마다 전체 수집 (목록 → 상세) */
    @Scheduled(cron = "0 0 */3 * * *")
    fun syncJobs() {
        logger.info("=== Starting job sync ===")
        try {
            jobSyncService.syncAll()
            logger.info("=== Job sync completed successfully ===")
        } catch (e: Exception) {
            logger.error("Error during job sync", e)
        }
    }
}
