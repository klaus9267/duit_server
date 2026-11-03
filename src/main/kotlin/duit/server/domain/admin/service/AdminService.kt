package duit.server.domain.admin.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.admin.dto.AdminPaginationParam
import duit.server.domain.admin.dto.AdminResponse
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
        val currentAdminId = adminRepository.findIdByUserId(currentUserId) ?: throw adminRequired()
        require(currentAdminId != id) { "본인의 관리자 권한은 해제할 수 없습니다" }

        // 두 행을 id 순서로 한 번에 잠가, 관리자끼리 동시에 해제해도 마지막 관리자가 사라지지 않고 락 순서가 고정된다
        val locked = adminRepository.findAllByIdForUpdate(listOf(currentAdminId, id)).associateBy { it.id }
        if (currentAdminId !in locked) throw adminRequired()
        val target = locked[id] ?: throw EntityNotFoundException("Admin을(를) 찾을 수 없습니다: $id")

        adminRepository.delete(target)
        logger.info(
            "관리자 해제: 요청자 userId={}, 대상 adminId={}, 대상 userId={}",
            currentUserId, id, target.user.id
        )
    }

    private fun adminRequired() = AccessDeniedException("관리자 권한이 필요합니다.")
}
