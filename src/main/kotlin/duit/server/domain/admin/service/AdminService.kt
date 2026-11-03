package duit.server.domain.admin.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.admin.dto.AdminPaginationParam
import duit.server.domain.admin.dto.AdminRegisterRequest
import duit.server.domain.admin.dto.AdminResponse
import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminService(
    private val adminRepository: AdminRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val securityUtil: SecurityUtil
) {

    private val logger = LoggerFactory.getLogger(AdminService::class.java)

    fun getAdmins(param: AdminPaginationParam): PageResponse<AdminResponse> =
        adminRepository.findAllAdmins(param.toPageable())
            .let { PageResponse(content = it.content, pageInfo = PageInfo.from(it)) }

    @Transactional
    fun register(request: AdminRegisterRequest): AdminResponse {
        val currentUserId = securityUtil.getCurrentUserId()
        lockAdmins(currentUserId)

        check(!adminRepository.existsByUserId(request.userId)) { "이미 관리자로 등록된 사용자입니다" }
        require(!adminRepository.existsByAdminId(request.adminId)) { "이미 존재하는 관리자 ID입니다" }

        val admin = adminRepository.save(
            Admin(
                user = userRepository.findByIdOrThrow(request.userId),
                adminId = request.adminId,
                password = passwordEncoder.encode(request.password)
            )
        )
        logger.info(
            "관리자 등록: 요청자 userId={}, 대상 userId={}, adminId(PK)={}",
            currentUserId, request.userId, admin.id
        )
        return AdminResponse.from(admin)
    }

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

    // 등록·해제는 admins 전체 행을 id 순서로 잠가 직렬화한다. DB 의 unique 제약에 기대지 않고 중복 등록과
    // "관리자끼리 동시 해제로 0명이 되는" 경합을 막으며, 락 순서가 고정이라 데드락이 없다.
    // 락을 얻은 뒤의 존재 여부 확인은 반드시 새로 조회한다 (대기 중 다른 트랜잭션이 넣은 행은 이 목록에 없을 수 있다).
    private fun lockAdmins(currentUserId: Long): List<Admin> =
        adminRepository.findAllForUpdate()
            .also { admins ->
                if (admins.none { it.user.id == currentUserId }) throw AccessDeniedException("관리자 권한이 필요합니다.")
            }
}
