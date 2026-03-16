package duit.server.domain.job.service

import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.JobSyncState
import duit.server.domain.job.entity.SourceType
import duit.server.domain.job.repository.JobPostingRepository
import duit.server.domain.job.repository.JobSyncStateRepository
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.job.JobFetcher
import duit.server.infrastructure.external.job.dto.IncrementalFetchResult
import duit.server.infrastructure.external.job.dto.JobFetchResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@Service
@Transactional(readOnly = true)
class JobSyncService(
    private val fetchers: List<JobFetcher>,
    private val jobPostingRepository: JobPostingRepository,
    private val jobSyncStateRepository: JobSyncStateRepository,
    private val discordService: DiscordService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun syncAll() {
        val now = LocalDateTime.now()
        val fetchResults = fetchAllAsync()

        fetchResults.forEach { (sourceType, results) ->
            upsertAll(sourceType, results)

            val syncState = jobSyncStateRepository.findById(sourceType).orElse(null)
            if (syncState != null) {
                syncState.lastSyncedAt = now
                syncState.lastFullSyncAt = now
            } else {
                jobSyncStateRepository.save(JobSyncState(sourceType, lastSyncedAt = now, lastFullSyncAt = now))
            }
        }

        deactivateExpiredPostings()
    }

    @Transactional
    fun syncIncremental() {
        val incrementalResults = fetchIncrementalAsync()

        incrementalResults.forEach { (sourceType, result) ->
            upsertAll(sourceType, result.items)

            if (result.latestTimestamp != null) {
                val syncState = jobSyncStateRepository.findById(sourceType).orElse(null)
                if (syncState != null) {
                    syncState.lastSyncedAt = result.latestTimestamp
                } else {
                    jobSyncStateRepository.save(JobSyncState(sourceType, lastSyncedAt = result.latestTimestamp))
                }
            }
        }

        deactivateExpiredPostings()
    }

    private fun fetchAllAsync(): List<Pair<SourceType, List<JobFetchResult>>> {
        val futures = fetchers.map { fetcher ->
            CompletableFuture.supplyAsync {
                try {
                    val results = fetcher.fetchAll()
                    log.info("Fetched ${results.size} job postings from ${fetcher.sourceType}")

                    if (results.isEmpty()) {
                        log.warn("${fetcher.sourceType}: fetchAll 결과가 0건")
                    }

                    fetcher.sourceType to results
                } catch (e: Exception) {
                    log.error("Failed to fetch job postings from ${fetcher.sourceType}", e)
                    discordService.sendServerErrorNotification(
                        errorCode = "JOB_SYNC_ERROR",
                        message = e.message ?: "Unknown error",
                        path = "JobSync/${fetcher.sourceType}",
                        timestamp = LocalDateTime.now(),
                        exception = e,
                    )
                    null
                }
            }
        }

        return futures.mapNotNull { it.join() }
    }

    private fun fetchIncrementalAsync(): List<Pair<SourceType, IncrementalFetchResult>> {
        val syncStates = jobSyncStateRepository.findAll().associateBy { it.sourceType }

        val futures = fetchers.map { fetcher ->
            CompletableFuture.supplyAsync {
                val syncState = syncStates[fetcher.sourceType]

                if (syncState == null) {
                    log.info("${fetcher.sourceType}: 워터마크 없음, fetchAll로 fallback")
                    try {
                        val results = fetcher.fetchAll()
                        log.info("Fetched ${results.size} job postings from ${fetcher.sourceType} (full fallback)")
                        val latestTimestamp = if (results.isNotEmpty()) LocalDateTime.now() else null
                        fetcher.sourceType to IncrementalFetchResult(results, latestTimestamp)
                    } catch (e: Exception) {
                        log.error("Failed to fetch job postings from ${fetcher.sourceType} (full fallback)", e)
                        discordService.sendServerErrorNotification(
                            errorCode = "JOB_SYNC_ERROR",
                            message = "Full fallback 실패: ${e.message}",
                            path = "JobSync/${fetcher.sourceType}",
                            timestamp = LocalDateTime.now(),
                            exception = e,
                        )
                        null
                    }
                } else {
                    val since = syncState.lastSyncedAt.minusMinutes(1)
                    try {
                        val result = fetcher.fetchIncremental(since)
                        log.info("Incremental fetch: ${result.items.size} items from ${fetcher.sourceType} (since=$since)")
                        fetcher.sourceType to result
                    } catch (e: Exception) {
                        log.error("Failed incremental fetch from ${fetcher.sourceType}", e)
                        discordService.sendServerErrorNotification(
                            errorCode = "JOB_SYNC_ERROR",
                            message = "Incremental fetch 실패: ${e.message}",
                            path = "JobSync/${fetcher.sourceType}",
                            timestamp = LocalDateTime.now(),
                            exception = e,
                        )
                        null
                    }
                }
            }
        }

        return futures.mapNotNull { it.join() }
    }

    private fun upsertAll(sourceType: SourceType, results: List<JobFetchResult>) {
        var insertCount = 0
        var updateCount = 0

        results.forEach { result ->
            val existing = jobPostingRepository.findBySourceTypeAndExternalId(sourceType, result.externalId)

            if (existing != null) {
                existing.updateFromSource(
                    title = result.title,
                    companyName = result.companyName,
                    jobCategory = result.jobCategory,
                    location = result.location,
                    workRegion = result.workRegion,
                    workDistrict = result.workDistrict,
                    employmentType = result.employmentType,
                    careerMin = result.careerMin,
                    careerMax = result.careerMax,
                    educationLevel = result.educationLevel,
                    salaryMin = result.salaryMin,
                    salaryMax = result.salaryMax,
                    salaryType = result.salaryType,
                    postingUrl = result.postingUrl,
                    postedAt = result.postedAt,
                    expiresAt = result.expiresAt,
                    closeType = result.closeType,
                    isActive = result.isActive,
                    workHoursPerWeek = result.workHoursPerWeek,
                )
                updateCount++
            } else {
                jobPostingRepository.save(
                    JobPosting(
                        sourceType = sourceType,
                        externalId = result.externalId,
                        title = result.title,
                        companyName = result.companyName,
                        jobCategory = result.jobCategory,
                        location = result.location,
                        workRegion = result.workRegion,
                        workDistrict = result.workDistrict,
                        employmentType = result.employmentType,
                        careerMin = result.careerMin,
                        careerMax = result.careerMax,
                        educationLevel = result.educationLevel,
                        salaryMin = result.salaryMin,
                        salaryMax = result.salaryMax,
                        salaryType = result.salaryType,
                        postingUrl = result.postingUrl,
                        postedAt = result.postedAt,
                        expiresAt = result.expiresAt,
                        closeType = result.closeType,
                        isActive = result.isActive,
                        workHoursPerWeek = result.workHoursPerWeek,
                    )
                )
                insertCount++
            }
        }

        log.info("${sourceType}: inserted=$insertCount, updated=$updateCount")
    }

    private fun deactivateExpiredPostings() {
        val now = LocalDateTime.now()
        val expired = jobPostingRepository.findByIsActiveTrueAndExpiresAtBefore(now)

        expired.forEach { it.syncActiveStatus(now) }

        if (expired.isNotEmpty()) {
            log.info("Deactivated ${expired.size} expired job postings")
        }
    }
}
