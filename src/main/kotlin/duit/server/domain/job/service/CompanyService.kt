package duit.server.domain.job.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.job.dto.JobCompanyResponse
import duit.server.domain.job.repository.CompanyBookmarkRepository
import duit.server.domain.job.repository.JobCompanyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CompanyService(
    private val jobCompanyRepository: JobCompanyRepository,
    private val companyBookmarkRepository: CompanyBookmarkRepository,
    private val securityUtil: SecurityUtil,
) {

    fun getCompanyDetail(companyId: Long): JobCompanyResponse {
        val company = jobCompanyRepository.findByIdOrThrow(companyId)
        val currentUserId = securityUtil.getCurrentUserIdOrNull()

        val isBookmarked = currentUserId?.let {
            companyBookmarkRepository.existsByCompanyIdAndUserId(companyId, it)
        } ?: false

        return JobCompanyResponse.from(company, isBookmarked = isBookmarked)
    }

    fun getBookmarkedCompanies(): List<JobCompanyResponse> {
        val currentUserId = securityUtil.getCurrentUserId()
        return companyBookmarkRepository.findBookmarkedCompaniesByUserId(currentUserId)
            .map { JobCompanyResponse.from(it, isBookmarked = true) }
    }
}
