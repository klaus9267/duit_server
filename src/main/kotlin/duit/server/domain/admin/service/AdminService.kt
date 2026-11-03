package duit.server.domain.admin.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.admin.dto.AdminPaginationParam
import duit.server.domain.admin.dto.AdminResponse
import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminService(
    private val adminRepository: AdminRepository,
    private val securityUtil: SecurityUtil
) {

    private val logger = LoggerFactory.getLogger(AdminService::class.java)

    fun getAdmins(param: AdminPaginationParam): PageResponse<AdminResponse> =
        adminRepository.findAllAdmins(param.toPageable())
            .let { PageResponse(content = it.content, pageInfo = PageInfo.from(it)) }

    @Transactional
    fun revokeAdmin(id: Long) {
        val currentUserId = securityUtil.getCurrentUserId()
        val admins = lockAdmins(currentUserId)

        val targetUserId = admins.firstOrNull { it.id == id }?.user?.id
            ?: throw EntityNotFoundException("Admin을(를) 찾을 수 없습니다: $id")
        require(targetUserId != currentUserId) { "본인의 관리자 권한은 해제할 수 없습니다" }

        adminRepository.deleteAllByUserId(targetUserId)
        logger.info("관리자 해제: 요청자 userId={}, 대상 adminId(PK)={}, 대상 userId={}", currentUserId, id, targetUserId)
    }

    // 해제는 admins 전체 행을 id 순서로 잠가 직렬화한다. "관리자끼리 동시 해제로 0명이 되는" 경합을 막으며,
    // 락 순서가 고정이라 데드락이 없다. 관리자 수가 극소수라 전체 행을 잠가도 비용이 거의 없다.
    private fun lockAdmins(currentUserId: Long): List<Admin> =
        adminRepository.findAllForUpdate()
            .also { admins ->
                if (admins.none { it.user.id == currentUserId }) throw AccessDeniedException("관리자 권한이 필요합니다.")
            }
}
