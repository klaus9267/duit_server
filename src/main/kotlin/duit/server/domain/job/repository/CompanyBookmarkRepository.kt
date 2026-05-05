package duit.server.domain.job.repository

import duit.server.domain.job.entity.Company
import duit.server.domain.job.entity.CompanyBookmark
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CompanyBookmarkRepository : JpaRepository<CompanyBookmark, Long> {

    fun findByCompanyIdAndUserId(companyId: Long, userId: Long): CompanyBookmark?

    fun existsByCompanyIdAndUserId(companyId: Long, userId: Long): Boolean

    @Query(
        """
        SELECT cb.company
        FROM CompanyBookmark cb
        WHERE cb.user.id = :userId
        ORDER BY cb.id DESC
        """
    )
    fun findBookmarkedCompaniesByUserId(userId: Long): List<Company>
}
