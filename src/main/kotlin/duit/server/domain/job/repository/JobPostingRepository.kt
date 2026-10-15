package duit.server.domain.job.repository

import duit.server.domain.job.entity.JobPosting
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface JobPostingRepository : JpaRepository<JobPosting, Long>, JobPostingRepositoryCustom {

    fun findByWantedAuthNo(wantedAuthNo: String): JobPosting?

    fun findByIdAndBelowMinimumWageFalse(id: Long): JobPosting?

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

/** 최저임금 미달 공고는 게재할 수 없으므로 존재하지 않는 공고와 같이 404 로 처리한다 */
fun JobPostingRepository.findPublishableByIdOrThrow(id: Long): JobPosting =
    findByIdAndBelowMinimumWageFalse(id)
        ?: throw EntityNotFoundException("JobPosting을(를) 찾을 수 없습니다: $id")
