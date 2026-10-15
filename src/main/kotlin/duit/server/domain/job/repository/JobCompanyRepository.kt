package duit.server.domain.job.repository

import duit.server.domain.job.entity.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface JobCompanyRepository : JpaRepository<Company, Long> {

    fun findByBusinessNumber(businessNumber: String): Company?

    fun findByCorpNm(corpNm: String): Company?

    /** 활성 공고가 있는 회사 중 임금체불 명단을 확인한 적 없거나 staleBefore 이전에 확인한 회사 (미확인 우선) */
    @Query(
        """
        SELECT c
        FROM Company c
        WHERE c.businessNumber IS NOT NULL
          AND (c.wageDefaulterCheckedAt IS NULL OR c.wageDefaulterCheckedAt < :staleBefore)
          AND EXISTS (SELECT 1 FROM JobPosting p WHERE p.company = c AND p.isActive = true)
        ORDER BY c.wageDefaulterCheckedAt ASC NULLS FIRST, c.id ASC
        """
    )
    fun findWageDefaulterCheckTargets(@Param("staleBefore") staleBefore: LocalDateTime): List<Company>

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE Company c
        SET c.wageDefaulter = :wageDefaulter, c.wageDefaulterCheckedAt = :checkedAt
        WHERE c.id = :id
        """
    )
    fun updateWageDefaulter(
        @Param("id") id: Long,
        @Param("wageDefaulter") wageDefaulter: Boolean,
        @Param("checkedAt") checkedAt: LocalDateTime,
    ): Int
}
