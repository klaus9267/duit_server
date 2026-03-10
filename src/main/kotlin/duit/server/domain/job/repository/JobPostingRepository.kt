package duit.server.domain.job.repository

import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.SourceType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface JobPostingRepository : JpaRepository<JobPosting, Long> {

    fun findBySourceTypeAndExternalId(sourceType: SourceType, externalId: String): JobPosting?

    fun findByIsActiveTrueAndExpiresAtBefore(now: LocalDateTime): List<JobPosting>
}
