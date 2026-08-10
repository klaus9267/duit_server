package duit.server.infrastructure.external.job

import duit.server.domain.job.entity.SourceType
import duit.server.infrastructure.external.job.dto.JobFetchBatch

interface JobFetcher {
    val sourceType: SourceType
    fun fetchAll(): JobFetchBatch
}
