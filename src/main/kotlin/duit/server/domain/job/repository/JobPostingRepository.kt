package duit.server.domain.job.repository

import duit.server.domain.job.entity.JobPosting
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface JobPostingRepository : JpaRepository<JobPosting, Long>, JobPostingRepositoryCustom {

    fun findByWantedAuthNo(wantedAuthNo: String): JobPosting?

    /**
     * 채용 상세: 공고·회사·첨부 목록을 SQL 1개로, 키워드 목록은 지연 로딩 1개로 (전에는 4개 — performance D-026).
     * 첨부·키워드는 둘 다 List(bag)라 한 번에 페치 조인하면 MultipleBagFetchException → 하나만 조인한다.
     */
    @EntityGraph(attributePaths = ["company", "corpAttachList"])
    fun findWithCompanyAndAttachListById(id: Long): JobPosting?

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
