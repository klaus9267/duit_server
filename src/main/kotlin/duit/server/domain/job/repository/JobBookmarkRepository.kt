package duit.server.domain.job.repository

import duit.server.domain.job.entity.JobBookmark
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface JobBookmarkRepository : JpaRepository<JobBookmark, Long> {

    fun findByJobPostingIdAndUserId(jobPostingId: Long, userId: Long): JobBookmark?

    @Query("SELECT jb.jobPosting.id FROM JobBookmark jb WHERE jb.user.id = :userId AND jb.jobPosting.id IN :jobPostingIds")
    fun findBookmarkedJobPostingIds(userId: Long, jobPostingIds: List<Long>): List<Long>
}
