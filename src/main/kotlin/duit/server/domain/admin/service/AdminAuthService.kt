package duit.server.domain.admin.service

import duit.server.application.security.JwtTokenProvider
import duit.server.domain.admin.dto.AdminLoginResponse
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.admin.repository.BannedIpRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminAuthService(
    private val adminRepository: AdminRepository,
    private val bannedIpRepository: BannedIpRepository,
    private val bannedIpService: BannedIpService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    private val logger = LoggerFactory.getLogger(AdminAuthService::class.java)

    @Transactional
    fun login(adminId: String, password: String, ip: String): AdminLoginResponse {
        // 1. IP 차단 확인
        bannedIpRepository.findByIpAddress(ip)
            ?.takeIf { it.isBanned }
            ?.let { throw IllegalStateException("IP가 차단되었습니다. 관리자에게 문의하세요.") }

        // 2. 관리자 조회 및 비밀번호 검증 (아이디/비밀번호 오류 구분 불가)
        val admin = findUniqueAdmin(adminId)
            ?.takeIf { passwordEncoder.matches(password, it.password) }
            ?: run {
                bannedIpService.handleLoginFailure(ip)
                throw IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다")
            }

        // 3. 로그인 성공 - JWT 발급
        val token = jwtTokenProvider.createAccessToken(admin.user.id!!)
        return AdminLoginResponse(token)
    }

    // admin_id 에 unique 제약이 없는 DB 에서 같은 ID 의 행이 둘 이상이면 어느 계정인지 알 수 없으므로 로그인을 거부한다
    private fun findUniqueAdmin(adminId: String) =
        adminRepository.findAllByAdminId(adminId).let { candidates ->
            if (candidates.size > 1) logger.warn("중복된 관리자 ID 로 로그인 시도: 일치하는 행 {}개", candidates.size)
            candidates.singleOrNull()
        }
}
