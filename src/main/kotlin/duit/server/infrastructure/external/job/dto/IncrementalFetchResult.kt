package duit.server.infrastructure.external.job.dto

import java.time.LocalDateTime

data class IncrementalFetchResult(
    val items: List<JobFetchResult>,
    /** 가장 최신 항목의 타임스탬프. 다음 증분 수집의 since 값으로 사용. */
    val latestTimestamp: LocalDateTime?,
)
