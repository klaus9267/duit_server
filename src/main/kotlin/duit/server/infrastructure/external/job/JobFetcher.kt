package duit.server.infrastructure.external.job

import duit.server.domain.job.entity.SourceType
import duit.server.infrastructure.external.job.dto.JobFetchResult

interface JobFetcher {
    val sourceType: SourceType
    fun fetchAll(): List<JobFetchResult>
}
