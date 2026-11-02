package duit.server.domain.job.repository

import duit.server.domain.job.entity.JobPosting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface JobPostingRepository : JpaRepository<JobPosting, Long>, JobPostingRepositoryCustom {

    fun findByWantedAuthNo(wantedAuthNo: String): JobPosting?

    fun countByIsActiveTrue(): Long

    @Query(
        """
        SELECT COUNT(jobPosting)
        FROM JobPosting jobPosting
        WHERE jobPosting.isActive = true
          AND jobPosting.wantedAuthNo NOT IN :activeExternalIds
          AND jobPosting.updatedAt <= :snapshotStartedAt
        """
    )
    fun countMissingActivePostings(
        @Param("activeExternalIds") activeExternalIds: Set<String>,
        @Param("snapshotStartedAt") snapshotStartedAt: LocalDateTime,
    ): Long

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE JobPosting jobPosting
        SET jobPosting.isActive = false, jobPosting.updatedAt = :snapshotStartedAt
        WHERE jobPosting.isActive = true
          AND jobPosting.wantedAuthNo NOT IN :activeExternalIds
          AND jobPosting.updatedAt <= :snapshotStartedAt
        """
    )
    fun deactivateMissingActivePostings(
        @Param("activeExternalIds") activeExternalIds: Set<String>,
        @Param("snapshotStartedAt") snapshotStartedAt: LocalDateTime,
    ): Int
}
