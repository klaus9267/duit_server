package duit.server.domain.admin.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.admin.dto.AdminPaginationParam
import duit.server.domain.admin.dto.AdminRegisterRequest
import duit.server.domain.admin.dto.AdminResponse
import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("AdminService 단위 테스트")
class AdminServiceUnitTest {

    private lateinit var adminRepository: AdminRepository
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var securityUtil: SecurityUtil
    private lateinit var adminService: AdminService

    private val currentUser = User(id = 1L, nickname = "요청자", providerId = "p-1")
    private val targetUser = User(id = 2L, nickname = "대상", providerId = "p-2")
    private val plainUser = User(id = 3L, nickname = "일반", providerId = "p-3", email = "plain@example.com")
    private val currentAdmin = Admin(id = 10L, user = currentUser, adminId = "current", password = "pw")
    private val targetAdmin = Admin(id = 20L, user = targetUser, adminId = "target", password = "pw")

    @BeforeEach
    fun setUp() {
        adminRepository = mockk()
        userRepository = mockk()
        passwordEncoder = mockk()
        securityUtil = mockk()
        adminService = AdminService(adminRepository, userRepository, passwordEncoder, securityUtil)

        every { securityUtil.getCurrentUserId() } returns currentUser.id!!
    }

    @Nested
    @DisplayName("getAdmins")
    inner class GetAdminsTests {

        @Test
        @DisplayName("등록 순 내림차순 고정 정렬로 조회하고 페이지 정보를 함께 반환한다")
        fun returnsPage() {
            val content = listOf(AdminResponse(20L, "target", 2L, "대상", null, LocalDateTime.now()))
            every { adminRepository.findAllAdmins(any()) } returns PageImpl(content, PageRequest.of(0, 10), 1)

            val result = adminService.getAdmins(AdminPaginationParam())

            assertEquals(content, result.content)
            assertEquals(1L, result.pageInfo.totalElements)
            verify(exactly = 1) {
                adminRepository.findAllAdmins(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")))
            }
        }

        @Test
        @DisplayName("페이지 크기는 100을 넘을 수 없다")
        fun clampsPageSize() {
            every { adminRepository.findAllAdmins(any()) } returns PageImpl(emptyList(), PageRequest.of(0, 100), 0)

            adminService.getAdmins(AdminPaginationParam(page = 0, size = 100_000))

            verify(exactly = 1) {
                adminRepository.findAllAdmins(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id")))
            }
        }
    }

    @Nested
    @DisplayName("register")
    inner class RegisterTests {

        private val request = AdminRegisterRequest(userId = 3L, adminId = "newadmin", password = "password123")

        @Test
        @DisplayName("요청 본문의 userId 사용자를 새로운 adminId의 관리자로 등록한다")
        fun registerSuccess() {
            val saved = slot<Admin>()
            every { adminRepository.findAllForUpdate() } returns listOf(currentAdmin, targetAdmin)
            every { adminRepository.existsByUserId(3L) } returns false
            every { adminRepository.existsByAdminId("newadmin") } returns false
            every { userRepository.findById(3L) } returns Optional.of(plainUser)
            every { passwordEncoder.encode("password123") } returns "encoded-new-pw"
            every { adminRepository.save(capture(saved)) } answers {
                Admin(id = 100L, user = saved.captured.user, adminId = saved.captured.adminId, password = saved.captured.password)
            }

            val result = adminService.register(request)

            assertEquals(100L, result.id)
            assertEquals("newadmin", result.adminId)
            assertEquals(3L, result.userId)
            assertEquals("일반", result.nickname)
            assertEquals("plain@example.com", result.email)
            assertEquals(plainUser, saved.captured.user)
            assertEquals("encoded-new-pw", saved.captured.password)
            verify(exactly = 0) { userRepository.findById(1L) }
        }

        @Test
        @DisplayName("요청자가 관리자가 아니면 AccessDeniedException이 발생하고 아무것도 저장하지 않는다")
        fun throwsWhenCallerIsNotAdmin() {
            every { adminRepository.findAllForUpdate() } returns listOf(targetAdmin)

            assertThrows<AccessDeniedException> { adminService.register(request) }

            verify(exactly = 0) { adminRepository.save(any<Admin>()) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 지정하면 EntityNotFoundException이 발생한다")
        fun throwsWhenUserNotFound() {
            every { adminRepository.findAllForUpdate() } returns listOf(currentAdmin)
            every { adminRepository.existsByUserId(3L) } returns false
            every { adminRepository.existsByAdminId("newadmin") } returns false
            every { userRepository.findById(3L) } returns Optional.empty()

            assertThrows<EntityNotFoundException> { adminService.register(request) }

            verify(exactly = 0) { adminRepository.save(any<Admin>()) }
        }

        @Test
        @DisplayName("이미 관리자인 사용자를 지정하면 IllegalStateException이 발생한다")
        fun throwsWhenUserAlreadyAdmin() {
            every { adminRepository.findAllForUpdate() } returns listOf(currentAdmin, targetAdmin)
            every { adminRepository.existsByUserId(2L) } returns true

            assertThrows<IllegalStateException> { adminService.register(request.copy(userId = 2L)) }

            verify(exactly = 0) { userRepository.findById(any()) }
            verify(exactly = 0) { adminRepository.save(any<Admin>()) }
        }

        @Test
        @DisplayName("이미 존재하는 adminId로 등록하면 IllegalArgumentException이 발생한다")
        fun throwsOnDuplicateAdminId() {
            every { adminRepository.findAllForUpdate() } returns listOf(currentAdmin)
            every { adminRepository.existsByUserId(3L) } returns false
            every { adminRepository.existsByAdminId("newadmin") } returns true

            assertThrows<IllegalArgumentException> { adminService.register(request) }

            verify(exactly = 0) { adminRepository.save(any<Admin>()) }
        }
    }

    @Nested
    @DisplayName("revokeAdmin")
    inner class RevokeAdminTests {

        @Test
        @DisplayName("다른 관리자를 해제하면 대상 사용자의 관리자 행만 삭제한다")
        fun revokeSuccess() {
            every { adminRepository.findAllForUpdate() } returns listOf(currentAdmin, targetAdmin)
            every { adminRepository.deleteAllByUserId(2L) } returns 1

            adminService.revokeAdmin(20L)

            verify(exactly = 1) { adminRepository.deleteAllByUserId(2L) }
            verify(exactly = 0) { adminRepository.deleteAllByUserId(1L) }
        }

        @Test
        @DisplayName("요청자 id 가 대상보다 커도 대상 행만 삭제한다")
        fun revokeSuccessWhenCallerHasHigherId() {
            val laterCaller = Admin(id = 30L, user = currentUser, adminId = "current", password = "pw")
            every { adminRepository.findAllForUpdate() } returns listOf(targetAdmin, laterCaller)
            every { adminRepository.deleteAllByUserId(2L) } returns 1

            adminService.revokeAdmin(20L)

            verify(exactly = 1) { adminRepository.deleteAllByUserId(2L) }
            verify(exactly = 0) { adminRepository.deleteAllByUserId(1L) }
        }

        @Test
        @DisplayName("행 id 가 아닌 대상 사용자 기준으로 삭제하므로 중복 행 중 어느 것을 지정해도 사용자의 관리자 행이 모두 사라진다")
        fun revokeDeletesByTargetUser() {
            val duplicate = Admin(id = 21L, user = targetUser, adminId = "target-2", password = "pw")
            every { adminRepository.findAllForUpdate() } returns listOf(currentAdmin, targetAdmin, duplicate)
            every { adminRepository.deleteAllByUserId(2L) } returns 2

            adminService.revokeAdmin(21L)

            verify(exactly = 1) { adminRepository.deleteAllByUserId(2L) }
        }

        @Test
        @DisplayName("본인의 관리자 권한을 해제하려 하면 IllegalArgumentException이 발생한다")
        fun throwsWhenRevokingSelf() {
            every { adminRepository.findAllForUpdate() } returns listOf(currentAdmin, targetAdmin)

            assertThrows<IllegalArgumentException> { adminService.revokeAdmin(10L) }

            verify(exactly = 0) { adminRepository.deleteAllByUserId(any()) }
        }

        @Test
        @DisplayName("본인 사용자에 중복된 관리자 행이 있어도 그 중 어느 행으로든 본인을 해제할 수 없다")
        fun throwsWhenRevokingSelfViaDuplicateRow() {
            val duplicate = Admin(id = 11L, user = currentUser, adminId = "current-2", password = "pw")
            every { adminRepository.findAllForUpdate() } returns listOf(currentAdmin, duplicate, targetAdmin)

            assertThrows<IllegalArgumentException> { adminService.revokeAdmin(11L) }

            verify(exactly = 0) { adminRepository.deleteAllByUserId(any()) }
        }

        @Test
        @DisplayName("요청자가 관리자가 아니면(락을 얻는 사이 해제된 경우 포함) AccessDeniedException이 발생한다")
        fun throwsWhenCallerIsNotAdmin() {
            every { adminRepository.findAllForUpdate() } returns listOf(targetAdmin)

            assertThrows<AccessDeniedException> { adminService.revokeAdmin(20L) }

            verify(exactly = 0) { adminRepository.deleteAllByUserId(any()) }
        }

        @Test
        @DisplayName("대상 관리자가 존재하지 않으면 EntityNotFoundException이 발생한다")
        fun throwsWhenTargetNotFound() {
            every { adminRepository.findAllForUpdate() } returns listOf(currentAdmin)

            assertThrows<EntityNotFoundException> { adminService.revokeAdmin(999L) }

            verify(exactly = 0) { adminRepository.deleteAllByUserId(any()) }
        }
    }
}
