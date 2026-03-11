package duit.server.domain.job.service

import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.SourceType
import duit.server.domain.job.repository.JobPostingRepository
import duit.server.infrastructure.external.job.JobFetcher
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
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun syncAll() {
        val fetchResults = fetchAllAsync()

        fetchResults.forEach { (sourceType, results) ->
            upsertAll(sourceType, results)
        }

        deactivateExpiredPostings()
    }

    private fun fetchAllAsync(): List<Pair<SourceType, List<JobFetchResult>>> {
        val futures = fetchers.map { fetcher ->
            CompletableFuture.supplyAsync {
                try {
                    val results = fetcher.fetchAll()
                    log.info("Fetched ${results.size} job postings from ${fetcher.sourceType}")
                    fetcher.sourceType to results
                } catch (e: Exception) {
                    log.error("Failed to fetch job postings from ${fetcher.sourceType}", e)
                    null
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
