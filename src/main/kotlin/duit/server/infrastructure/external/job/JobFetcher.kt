package duit.server.infrastructure.external.job

import duit.server.domain.job.entity.SourceType
import duit.server.infrastructure.external.job.dto.IncrementalFetchResult
import duit.server.infrastructure.external.job.dto.JobFetchResult
import java.time.LocalDateTime

interface JobFetcher {
    val sourceType: SourceType
    fun fetchAll(): List<JobFetchResult>
    fun fetchIncremental(since: LocalDateTime): IncrementalFetchResult
}
