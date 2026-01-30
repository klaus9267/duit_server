package duit.server.domain.admin.service

import duit.server.application.security.JwtTokenProvider
import duit.server.application.security.SecurityUtil
import duit.server.domain.admin.dto.AdminLoginResponse
import duit.server.domain.admin.dto.AdminRegisterRequest
import duit.server.domain.admin.dto.AdminResponse
import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.admin.repository.BannedIpRepository
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminAuthService(
    private val adminRepository: AdminRepository,
    private val bannedIpRepository: BannedIpRepository,
    private val bannedIpService: BannedIpService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val securityUtil: SecurityUtil
) {

    @Transactional
    fun login(adminId: String, password: String, ip: String): AdminLoginResponse {
        // 1. IP 차단 확인
        bannedIpRepository.findByIpAddress(ip)
            ?.takeIf { it.isBanned }
            ?.let { throw IllegalStateException("IP가 차단되었습니다. 관리자에게 문의하세요.") }

        // 2. 관리자 조회 및 비밀번호 검증 (아이디/비밀번호 오류 구분 불가)
        val admin = adminRepository.findByAdminId(adminId)
            ?.takeIf { passwordEncoder.matches(password, it.password) }
            ?: run {
                bannedIpService.handleLoginFailure(ip)
                throw IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다")
            }

        // 3. 로그인 성공 - JWT 발급
        val token = jwtTokenProvider.createAccessToken(admin.user.id!!)
        return AdminLoginResponse(token)
    }

    @Transactional
    fun register(request: AdminRegisterRequest): AdminResponse {
        require(!adminRepository.existsByAdminId(request.adminId)) {
            "이미 존재하는 관리자 ID입니다"
        }

        val currentUserId = securityUtil.getCurrentUserId()

        return userRepository.findByIdOrThrow(currentUserId, "사용자")
            .let { user ->
                val admin = Admin(
                    user = user,
                    adminId = request.adminId,
                    password = passwordEncoder.encode(request.password)
                )
                adminRepository.save(admin)
                AdminResponse.from(admin)
            }
    }
}
