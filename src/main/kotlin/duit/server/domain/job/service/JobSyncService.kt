package duit.server.domain.job.service

import duit.server.domain.job.entity.Company
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.JobSyncState
import duit.server.domain.job.repository.JobCompanyRepository
import duit.server.domain.job.repository.JobPostingRepository
import duit.server.domain.job.repository.JobSyncStateRepository
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.job.JobFetcher
import duit.server.infrastructure.external.job.dto.CompanyFetchResult
import duit.server.infrastructure.external.job.dto.JobFetchResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

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

        fetchers.forEach { fetcher ->
            runCatching {
                val results = fetcher.fetchAll()
                log.info("Fetched ${results.size} job postings from ${fetcher.sourceType}")
                upsertAll(results)

                jobSyncStateRepository.findById(fetcher.sourceType).orElse(null)?.apply {
                    lastSyncedAt = now
                    lastFullSyncAt = now
                } ?: jobSyncStateRepository.save(
                    JobSyncState(fetcher.sourceType, lastSyncedAt = now, lastFullSyncAt = now)
                )
            }.onFailure { reportFetchFailure(fetcher, it) }
        }
    }

    private fun reportFetchFailure(fetcher: JobFetcher, e: Throwable) {
        log.error("Failed fetch from ${fetcher.sourceType}", e)
        discordService.sendServerErrorNotification(
            errorCode = "JOB_SYNC_ERROR",
            message = e.message ?: "Unknown error",
            path = "JobSync/${fetcher.sourceType}",
            timestamp = LocalDateTime.now(),
            exception = e as? Exception ?: RuntimeException(e),
        )
    }

    private fun upsertAll(results: List<JobFetchResult>) {
        var insertCount = 0
        var updateCount = 0

        results.forEach { result ->
            val company = resolveCompany(result.company)
            val existing = jobPostingRepository.findByWantedAuthNo(result.externalId)

            if (existing != null) {
                existing.isActive = result.isActive
                existing.updateWork24Detail(detail = result.detail, company = company)
                updateCount++
            } else {
                jobPostingRepository.save(
                    JobPosting(
                        wantedAuthNo = result.externalId,
                        isActive = result.isActive,
                    ).apply {
                        updateWork24Detail(detail = result.detail, company = company)
                    }
                )
                insertCount++
            }
        }

        log.info("job_postings: inserted=$insertCount, updated=$updateCount")
    }

    private fun resolveCompany(profile: CompanyFetchResult): Company? {
        val corpNm = profile.corpNm?.takeIf { it.isNotBlank() }
        val bizNo = profile.businessNumber?.takeIf { it.isNotBlank() }

        val company = when {
            bizNo != null -> {
                jobCompanyRepository.findByBusinessNumber(bizNo)
                    ?: corpNm?.let { jobCompanyRepository.findByCorpNm(it) }?.apply {
                        businessNumber = bizNo
                    }
                    ?: jobCompanyRepository.save(Company(businessNumber = bizNo, corpNm = corpNm))
            }
            corpNm != null -> {
                jobCompanyRepository.findByCorpNm(corpNm)
                    ?: jobCompanyRepository.save(Company(corpNm = corpNm))
            }
            else -> null
        } ?: return null

        if (corpNm != null && company.corpNm != corpNm) company.corpNm = corpNm
        profile.reperNm?.let { company.reperNm = it }
        profile.totPsncnt?.let { company.totPsncnt = it }
        profile.capitalAmt?.let { company.capitalAmt = it }
        profile.yrSalesAmt?.let { company.yrSalesAmt = it }
        profile.indTpCdNm?.let { company.indTpCdNm = it }
        profile.busiCont?.let { company.busiCont = it }
        profile.corpAddr?.let { company.corpAddr = it }
        profile.homePg?.let { company.homePg = it }
        profile.busiSize?.let { company.busiSize = it }

        return company
    }
}
