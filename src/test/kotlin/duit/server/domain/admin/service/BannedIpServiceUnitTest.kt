package duit.server.domain.admin.service

import duit.server.domain.admin.entity.BannedIp
import duit.server.domain.admin.repository.BannedIpRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BannedIpService 단위 테스트")
class BannedIpServiceUnitTest {

    private lateinit var bannedIpRepository: BannedIpRepository
    private lateinit var bannedIpService: BannedIpService

    @BeforeEach
    fun setUp() {
        bannedIpRepository = mockk()
        bannedIpService = BannedIpService(bannedIpRepository)
    }

    @Nested
    @DisplayName("handleLoginFailure")
    inner class HandleLoginFailureTests {

        @Test
        @DisplayName("새로운 IP이면 failureCount=1로 BannedIp를 생성한다")
        fun createsNewBannedIp() {
            val savedSlot = slot<BannedIp>()
            every { bannedIpRepository.findByIpAddress("10.0.0.1") } returns null
            every { bannedIpRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            bannedIpService.handleLoginFailure("10.0.0.1")

            assertEquals("10.0.0.1", savedSlot.captured.ipAddress)
            assertEquals(1, savedSlot.captured.failureCount)
        }

        @Test
        @DisplayName("기존 IP이면 recordFailure를 호출하고 저장한다")
        fun updatesExistingBannedIp() {
            val existingIp = spyk(
                BannedIp(id = 1L, ipAddress = "10.0.0.1", failureCount = 2)
            )
            every { bannedIpRepository.findByIpAddress("10.0.0.1") } returns existingIp
            every { bannedIpRepository.save(existingIp) } returns existingIp

            bannedIpService.handleLoginFailure("10.0.0.1")

            verify(exactly = 1) { existingIp.recordFailure() }
            verify(exactly = 1) { bannedIpRepository.save(existingIp) }
        }
    }
}
