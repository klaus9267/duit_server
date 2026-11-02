package duit.server.domain.job.service

import duit.server.domain.job.entity.Company
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.JobSyncState
import duit.server.domain.job.repository.JobCompanyRepository
import duit.server.domain.job.repository.JobPostingRepository
import duit.server.domain.job.repository.JobSyncStateRepository
import duit.server.domain.subscription.service.SubscriptionNotificationService
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.job.JobFetcher
import duit.server.infrastructure.external.job.dto.CompanyFetchResult
import duit.server.infrastructure.external.job.dto.JobFetchBatch
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
    private val subscriptionNotificationService: SubscriptionNotificationService,
) {
    companion object {
        private const val SNAPSHOT_DROP_GUARD_FACTOR = 2L
    }

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun syncAll() {
        val now = LocalDateTime.now()

        fetchers.forEach { fetcher ->
            runCatching {
                val batch = fetcher.fetchAll()
                log.info("Fetched ${batch.postings.size} job postings from ${fetcher.sourceType}")
                val shouldReconcileActiveSnapshot = shouldReconcileActiveSnapshot(fetcher, batch, now)
                upsertAll(batch.postings)
                val reconciledFullSnapshot = shouldReconcileActiveSnapshot &&
                    reconcileActiveSnapshot(fetcher, batch, now)

                jobSyncStateRepository.findById(fetcher.sourceType).orElse(null)?.apply {
                    lastSyncedAt = now
                    if (reconciledFullSnapshot) lastFullSyncAt = now
                } ?: jobSyncStateRepository.save(
                    JobSyncState(
                        fetcher.sourceType,
                        lastSyncedAt = now,
                        lastFullSyncAt = now.takeIf { reconciledFullSnapshot },
                    )
                )
            }.onFailure { reportFetchFailure(fetcher, it) }
        }
    }

    private fun reconcileActiveSnapshot(
        fetcher: JobFetcher,
        batch: JobFetchBatch,
        now: LocalDateTime,
    ): Boolean {
        val deactivated = jobPostingRepository.deactivateMissingActivePostings(batch.activeExternalIds, now)
        log.info(
            "{} active snapshot 정합화 완료: activeIds={}, deactivated={}",
            fetcher.sourceType,
            batch.activeExternalIds.size,
            deactivated,
        )
        return true
    }

    private fun shouldReconcileActiveSnapshot(
        fetcher: JobFetcher,
        batch: JobFetchBatch,
        now: LocalDateTime,
    ): Boolean {
        val skipReason = when {
            fetchers.size != 1 -> "복수 소스에서 공고 소스를 구분할 수 없음"
            !batch.isCompleteSnapshot -> "전체 active ID snapshot이 불완전함"
            batch.activeExternalIds.isEmpty() -> "active ID snapshot이 비어 있음"
            else -> null
        }

        if (skipReason != null) {
            log.warn("{} 누락 공고 비활성화 건너뜀: {}", fetcher.sourceType, skipReason)
            return false
        }

        val currentActiveCount = jobPostingRepository.countByIsActiveTrue()
        val missingActiveCount = jobPostingRepository.countMissingActivePostings(batch.activeExternalIds, now)
        if (missingActiveCount * SNAPSHOT_DROP_GUARD_FACTOR > currentActiveCount) {
            reportSuspiciousSnapshotDrop(
                fetcher = fetcher,
                snapshotSize = batch.activeExternalIds.size,
                missingActiveCount = missingActiveCount,
                currentActiveCount = currentActiveCount,
                now = now,
            )
            return false
        }

        return true
    }

    private fun reportSuspiciousSnapshotDrop(
        fetcher: JobFetcher,
        snapshotSize: Int,
        missingActiveCount: Long,
        currentActiveCount: Long,
        now: LocalDateTime,
    ) {
        val message = "active snapshot 급감 감지: " +
            "snapshot=$snapshotSize, missingActive=$missingActiveCount, currentActive=$currentActiveCount"
        log.error("{} 누락 공고 비활성화 건너뜀: {}", fetcher.sourceType, message)
        discordService.sendServerErrorNotification(
            errorCode = "JOB_SYNC_SNAPSHOT_DROP",
            message = message,
            path = "JobSync/${fetcher.sourceType}/active-snapshot",
            timestamp = now,
            exception = IllegalStateException(message),
        )
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
                val saved = jobPostingRepository.save(
                    JobPosting(
                        wantedAuthNo = result.externalId,
                        isActive = result.isActive,
                    ).apply {
                        updateWork24Detail(detail = result.detail, company = company)
                    }
                )
                insertCount++
                if (saved.isActive) {
                    subscriptionNotificationService.notifyOnJobPostingCreated(saved)
                }
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
