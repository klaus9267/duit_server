package duit.server.domain.job.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.dto.pagination.CursorPageInfo
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.job.dto.JobPostingCursor
import duit.server.domain.job.dto.JobPostingCursorPaginationParam
import duit.server.domain.job.dto.JobPostingResponse
import duit.server.domain.job.dto.encode
import duit.server.domain.job.repository.JobBookmarkRepository
import duit.server.domain.job.repository.JobPostingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class JobPostingService(
    private val jobPostingRepository: JobPostingRepository,
    private val jobBookmarkRepository: JobBookmarkRepository,
    private val securityUtil: SecurityUtil,
) {
    fun getJobPostings(param: JobPostingCursorPaginationParam): CursorPageResponse<JobPostingResponse> {
        val currentUserId = securityUtil.getCurrentUserIdOrNull()

        val jobPostings = jobPostingRepository.findJobPostings(param, currentUserId)

        val hasNext = jobPostings.size > param.size
        val actualJobPostings = if (hasNext) jobPostings.dropLast(1) else jobPostings

        val nextCursor = if (hasNext && actualJobPostings.isNotEmpty()) {
            JobPostingCursor.fromJobPosting(actualJobPostings.last(), param.field).encode()
        } else null

        val responses = if (currentUserId != null && actualJobPostings.isNotEmpty()) {
            val ids = actualJobPostings.map { it.id!! }
            val bookmarkedIds = jobBookmarkRepository.findBookmarkedJobPostingIds(currentUserId, ids).toSet()
            actualJobPostings.map { JobPostingResponse.from(it, it.id in bookmarkedIds) }
        } else {
            actualJobPostings.map { JobPostingResponse.from(it) }
        }

        return CursorPageResponse(
            content = responses,
            pageInfo = CursorPageInfo(
                hasNext = hasNext,
                nextCursor = nextCursor,
                pageSize = actualJobPostings.size
            )
        )
    }

    fun getJobPostingDetail(jobPostingId: Long): JobPostingResponse {
        val jobPosting = jobPostingRepository.findByIdOrThrow(jobPostingId)
        val currentUserId = securityUtil.getCurrentUserIdOrNull()

        val isBookmarked = currentUserId?.let {
            jobBookmarkRepository.findBookmarkedJobPostingIds(it, listOf(jobPosting.id!!)).isNotEmpty()
        } ?: false

        return JobPostingResponse.from(jobPosting, isBookmarked)
    }
}
