package duit.server.domain.admin.service

import duit.server.application.security.JwtTokenProvider
import duit.server.application.security.SecurityUtil
import duit.server.domain.admin.dto.AdminRegisterRequest
import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.entity.BannedIp
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.admin.repository.BannedIpRepository
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@DisplayName("AdminAuthService 단위 테스트")
class AdminAuthServiceUnitTest {

    private lateinit var adminRepository: AdminRepository
    private lateinit var bannedIpRepository: BannedIpRepository
    private lateinit var bannedIpService: BannedIpService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var securityUtil: SecurityUtil
    private lateinit var adminAuthService: AdminAuthService

    private lateinit var user: User
    private lateinit var admin: Admin

    @BeforeEach
    fun setUp() {
        adminRepository = mockk()
        bannedIpRepository = mockk()
        bannedIpService = mockk(relaxed = true)
        userRepository = mockk()
        passwordEncoder = mockk()
        jwtTokenProvider = mockk()
        securityUtil = mockk()
        adminAuthService = AdminAuthService(
            adminRepository, bannedIpRepository, bannedIpService,
            userRepository, passwordEncoder, jwtTokenProvider, securityUtil
        )

        user = User(id = 1L, nickname = "관리자", providerId = "admin-p1")
        admin = Admin(id = 1L, user = user, adminId = "testadmin", password = "encoded-pw")
    }

    @Nested
    @DisplayName("login")
    inner class LoginTests {

        @Test
        @DisplayName("올바른 자격증명으로 로그인하면 토큰을 반환한다")
        fun loginSuccess() {
            every { bannedIpRepository.findByIpAddress("192.168.1.1") } returns null
            every { adminRepository.findByAdminId("testadmin") } returns admin
            every { passwordEncoder.matches("password123", "encoded-pw") } returns true
            every { jwtTokenProvider.createAccessToken(1L) } returns "jwt-token"

            val result = adminAuthService.login("testadmin", "password123", "192.168.1.1")

            assertEquals("jwt-token", result.accessToken)
        }

        @Test
        @DisplayName("차단된 IP에서 로그인하면 IllegalStateException이 발생한다")
        fun throwsOnBannedIp() {
            val bannedIp = BannedIp(ipAddress = "192.168.1.1", isBanned = true, failureCount = 5)
            every { bannedIpRepository.findByIpAddress("192.168.1.1") } returns bannedIp

            assertThrows<IllegalStateException> {
                adminAuthService.login("testadmin", "password123", "192.168.1.1")
            }
        }

        @Test
        @DisplayName("IP 기록이 있지만 차단되지 않았으면 정상 진행한다")
        fun proceedsWithUnbannedIp() {
            val unbannedIp = BannedIp(ipAddress = "192.168.1.1", isBanned = false, failureCount = 2)
            every { bannedIpRepository.findByIpAddress("192.168.1.1") } returns unbannedIp
            every { adminRepository.findByAdminId("testadmin") } returns admin
            every { passwordEncoder.matches("password123", "encoded-pw") } returns true
            every { jwtTokenProvider.createAccessToken(1L) } returns "jwt-token"

            val result = adminAuthService.login("testadmin", "password123", "192.168.1.1")

            assertEquals("jwt-token", result.accessToken)
        }

        @Test
        @DisplayName("존재하지 않는 adminId로 로그인하면 실패 처리 후 IllegalArgumentException이 발생한다")
        fun throwsOnWrongAdminId() {
            every { bannedIpRepository.findByIpAddress("192.168.1.1") } returns null
            every { adminRepository.findByAdminId("wrongadmin") } returns null

            assertThrows<IllegalArgumentException> {
                adminAuthService.login("wrongadmin", "password123", "192.168.1.1")
            }
            verify(exactly = 1) { bannedIpService.handleLoginFailure("192.168.1.1") }
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인하면 실패 처리 후 IllegalArgumentException이 발생한다")
        fun throwsOnWrongPassword() {
            every { bannedIpRepository.findByIpAddress("192.168.1.1") } returns null
            every { adminRepository.findByAdminId("testadmin") } returns admin
            every { passwordEncoder.matches("wrongpw", "encoded-pw") } returns false

            assertThrows<IllegalArgumentException> {
                adminAuthService.login("testadmin", "wrongpw", "192.168.1.1")
            }
            verify(exactly = 1) { bannedIpService.handleLoginFailure("192.168.1.1") }
        }
    }

    @Nested
    @DisplayName("register")
    inner class RegisterTests {

        @Test
        @DisplayName("새로운 adminId로 등록하면 AdminResponse를 반환한다")
        fun registerSuccess() {
            every { adminRepository.existsByAdminId("newadmin") } returns false
            every { securityUtil.getCurrentUserId() } returns 1L
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { passwordEncoder.encode("password123") } returns "encoded-new-pw"
            every { adminRepository.save(any<Admin>()) } answers {
                val arg = firstArg<Admin>()
                Admin(id = 100L, user = arg.user, adminId = arg.adminId, password = arg.password)
            }

            val result = adminAuthService.register(AdminRegisterRequest("newadmin", "password123"))

            assertEquals("newadmin", result.adminId)
            assertEquals(1L, result.userId)
        }

        @Test
        @DisplayName("이미 존재하는 adminId로 등록하면 IllegalArgumentException이 발생한다")
        fun throwsOnDuplicateAdminId() {
            every { adminRepository.existsByAdminId("testadmin") } returns true

            assertThrows<IllegalArgumentException> {
                adminAuthService.register(AdminRegisterRequest("testadmin", "password123"))
            }
        }
    }
}
