package duit.server.domain.job.repository

import duit.server.domain.job.dto.JobPostingCursor
import duit.server.domain.job.dto.JobPostingCursorPaginationParam
import duit.server.domain.job.entity.JobPosting

interface JobPostingRepositoryCustom {
    fun findJobPostings(
        param: JobPostingCursorPaginationParam,
        currentUserId: Long?,
        cursor: JobPostingCursor?,
    ): List<JobPosting>
}
