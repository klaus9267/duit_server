package duit.server.domain.job.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.job.dto.CompanyBookmarkToggleResponse
import duit.server.domain.job.entity.CompanyBookmark
import duit.server.domain.job.repository.CompanyBookmarkRepository
import duit.server.domain.job.repository.JobCompanyRepository
import duit.server.domain.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CompanyBookmarkService(
    private val companyBookmarkRepository: CompanyBookmarkRepository,
    private val jobCompanyRepository: JobCompanyRepository,
    private val userService: UserService,
    private val securityUtil: SecurityUtil,
) {

    @Transactional
    fun toggleBookmark(companyId: Long): CompanyBookmarkToggleResponse {
        val currentUserId = securityUtil.getCurrentUserId()
        val existing = companyBookmarkRepository.findByCompanyIdAndUserId(companyId, currentUserId)

        val isBookmarked = if (existing != null) {
            companyBookmarkRepository.delete(existing)
            false
        } else {
            val company = jobCompanyRepository.findByIdOrThrow(companyId)
            val user = userService.findUserById(currentUserId)
            companyBookmarkRepository.save(CompanyBookmark(user = user, company = company))
            true
        }

        return CompanyBookmarkToggleResponse(companyId = companyId, isBookmarked = isBookmarked)
    }
}
