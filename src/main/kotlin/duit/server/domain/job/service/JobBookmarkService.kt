package duit.server.domain.job.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.job.dto.JobBookmarkToggleResponse
import duit.server.domain.job.entity.JobBookmark
import duit.server.domain.job.repository.JobBookmarkRepository
import duit.server.domain.job.repository.JobPostingRepository
import duit.server.domain.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class JobBookmarkService(
    private val jobBookmarkRepository: JobBookmarkRepository,
    private val jobPostingRepository: JobPostingRepository,
    private val userService: UserService,
    private val securityUtil: SecurityUtil,
) {
    @Transactional
    fun toggleBookmark(jobPostingId: Long): JobBookmarkToggleResponse {
        val currentUserId = securityUtil.getCurrentUserId()
        val existingBookmark = jobBookmarkRepository.findByJobPostingIdAndUserId(jobPostingId, currentUserId)

        val isBookmarked = if (existingBookmark != null) {
            jobBookmarkRepository.delete(existingBookmark)
            false
        } else {
            val jobPosting = jobPostingRepository.findByIdOrThrow(jobPostingId)
            require(jobPosting.isActive) { "비활성 채용공고입니다" }

            val user = userService.findUserById(currentUserId)
            jobBookmarkRepository.save(JobBookmark(user = user, jobPosting = jobPosting))
            true
        }

        return JobBookmarkToggleResponse(jobPostingId, isBookmarked)
    }
}
