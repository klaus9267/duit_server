package duit.server.domain.job.service

import duit.server.domain.job.entity.*
import duit.server.domain.job.repository.JobCompanyRepository
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
    private val jobCompanyRepository: JobCompanyRepository,
    private val jobPostingRepository: JobPostingRepository,
    private val jobSyncStateRepository: JobSyncStateRepository,
    private val discordService: DiscordService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun syncAll() {
        val now = LocalDateTime.now()

        fetchAllAsync().forEach { (sourceType, results) ->
            upsertAll(results)

            jobSyncStateRepository.findById(sourceType).orElse(null)?.apply {
                lastSyncedAt = now
                lastFullSyncAt = now
            } ?: jobSyncStateRepository.save(
                JobSyncState(sourceType, lastSyncedAt = now, lastFullSyncAt = now)
            )
        }
    }

    @Transactional
    fun syncIncremental() {
        fetchIncrementalAsync().forEach { (sourceType, result) ->
            upsertAll(result.items)

            result.latestTimestamp?.let { latestTimestamp ->
                jobSyncStateRepository.findById(sourceType).orElse(null)?.apply {
                    lastSyncedAt = latestTimestamp
                } ?: jobSyncStateRepository.save(
                    JobSyncState(sourceType, lastSyncedAt = latestTimestamp)
                )
            }
        }
    }

    private fun fetchAllAsync(): List<Pair<SourceType, List<JobFetchResult>>> =
        fetchers.map { fetcher ->
            CompletableFuture.supplyAsync {
                try {
                    val results = fetcher.fetchAll()
                    log.info("Fetched ${results.size} job postings from ${fetcher.sourceType}")
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
        }.mapNotNull { it.join() }

    private fun fetchIncrementalAsync(): List<Pair<SourceType, IncrementalFetchResult>> {
        val syncStates = jobSyncStateRepository.findAll().associateBy { it.sourceType }

        return fetchers.map { fetcher ->
            CompletableFuture.supplyAsync {
                val syncState = syncStates[fetcher.sourceType]

                try {
                    if (syncState == null) {
                        val results = fetcher.fetchAll()
                        log.info("${fetcher.sourceType}: 워터마크 없음, fetchAll로 fallback")
                        fetcher.sourceType to IncrementalFetchResult(
                            items = results,
                            latestTimestamp = results.takeIf { it.isNotEmpty() }?.let { LocalDateTime.now() },
                        )
                    } else {
                        val since = syncState.lastSyncedAt.minusMinutes(1)
                        val result = fetcher.fetchIncremental(since)
                        log.info("Incremental fetch: ${result.items.size} items from ${fetcher.sourceType} (since=$since)")
                        fetcher.sourceType to result
                    }
                } catch (e: Exception) {
                    log.error("Failed fetch from ${fetcher.sourceType}", e)
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
        }.mapNotNull { it.join() }
    }

    private fun upsertAll(results: List<JobFetchResult>) {
        var insertCount = 0
        var updateCount = 0

        results.forEach { result ->
            val existing = jobPostingRepository.findByWantedAuthNo(result.externalId)
            val company = when {
                result.businessNumber.isNullOrBlank() -> result.companyName
                    .takeIf { it.isNotBlank() }
                    ?.let { corpNm -> jobCompanyRepository.findByCorpNm(corpNm) ?: jobCompanyRepository.save(JobCompany(corpNm = corpNm)) }

                else -> jobCompanyRepository.findByBusinessNumber(result.businessNumber)
                    ?: jobCompanyRepository.findByCorpNm(result.companyName)?.apply {
                        businessNumber = result.businessNumber
                    }
                    ?: jobCompanyRepository.save(
                        JobCompany(
                            businessNumber = result.businessNumber,
                            corpNm = result.companyName,
                        )
                    )
            }?.apply {
                if (corpNm != result.companyName) {
                    corpNm = result.companyName
                }
            }

            if (existing != null) {
                existing.isActive = result.isActive
                existing.updateWork24Detail(
                    detail = result.toWork24Detail(),
                    company = company,
                )
                updateCount++
            } else {
                jobPostingRepository.save(
                    JobPosting(
                        wantedAuthNo = result.externalId,
                        isActive = result.isActive,
                    ).apply {
                        updateWork24Detail(
                            detail = result.toWork24Detail(),
                            company = company,
                        )
                    }
                )
                insertCount++
            }
        }

        log.info("job_postings: inserted=$insertCount, updated=$updateCount")
    }

    private fun JobFetchResult.toWork24Detail() = JobPostingWork24Detail(
        jobsNm = jobCategory,
        wantedTitle = title,
        receiptCloseDt = expiresAt?.toString(),
        empTpNm = employmentType?.displayName,
        salTpNm = salaryType?.displayName,
        enterTpNm = when {
            careerMin == null && careerMax == null -> null
            careerMin == 0 && careerMax == null -> "신입"
            careerMin != null && careerMax == null -> "경력 ${careerMin}년 이상"
            careerMin != null && careerMax != null -> "경력 ${careerMin}~${careerMax}년"
            else -> "경력무관"
        },
        eduNm = educationLevel?.displayName,
        workRegion = location,
        dtlRecrContUrl = postingUrl,
        jobsCd = jobCategory,
        empTpCd = employmentType?.name,
        salTpCd = salaryType?.name,
    )
}
