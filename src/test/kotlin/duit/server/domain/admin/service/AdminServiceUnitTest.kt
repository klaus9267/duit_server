package duit.server.domain.admin.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.admin.dto.AdminPaginationParam
import duit.server.domain.admin.dto.AdminResponse
import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.user.entity.User
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalDateTime

@DisplayName("AdminService 단위 테스트")
class AdminServiceUnitTest {

    private lateinit var adminRepository: AdminRepository
    private lateinit var securityUtil: SecurityUtil
    private lateinit var adminService: AdminService

    private val currentUser = User(id = 1L, nickname = "요청자", providerId = "p-1")
    private val targetUser = User(id = 2L, nickname = "대상", providerId = "p-2")
    private val currentAdmin = Admin(id = 10L, user = currentUser, adminId = "current", password = "pw")
    private val targetAdmin = Admin(id = 20L, user = targetUser, adminId = "target", password = "pw")

    @BeforeEach
    fun setUp() {
        adminRepository = mockk()
        securityUtil = mockk()
        adminService = AdminService(adminRepository, securityUtil)

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
