package duit.server.domain.admin.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("BannedIp 엔티티 단위 테스트")
class BannedIpUnitTest {

    @Nested
    @DisplayName("ban")
    inner class BanTests {

        @Test
        @DisplayName("ban 호출 시 isBanned가 true가 된다")
        fun banSetsTrue() {
            val bannedIp = BannedIp(ipAddress = "192.168.1.1")
            bannedIp.ban()
            assertTrue(bannedIp.isBanned)
        }
    }

    @Nested
    @DisplayName("unban")
    inner class UnbanTests {

        @Test
        @DisplayName("unban 호출 시 isBanned=false, failureCount=0이 된다")
        fun unbanResetsState() {
            val bannedIp = BannedIp(ipAddress = "192.168.1.1", isBanned = true, failureCount = 5)
            bannedIp.unban()

            assertFalse(bannedIp.isBanned)
            assertEquals(0, bannedIp.failureCount)
        }
    }

    @Nested
    @DisplayName("recordFailure")
    inner class RecordFailureTests {

        @Test
        @DisplayName("24시간 이내 실패 → failureCount 증가")
        fun incrementsWithin24Hours() {
            val bannedIp = BannedIp(
                ipAddress = "192.168.1.1",
                failureCount = 2,
                lastFailureAt = LocalDateTime.now().minusHours(1)
            )
            bannedIp.recordFailure()

            assertEquals(3, bannedIp.failureCount)
            assertFalse(bannedIp.isBanned)
        }

        @Test
        @DisplayName("24시간 초과 후 실패 → failureCount 1로 리셋")
        fun resetsAfter24Hours() {
            val bannedIp = BannedIp(
                ipAddress = "192.168.1.1",
                failureCount = 4,
                lastFailureAt = LocalDateTime.now().minusHours(25)
            )
            bannedIp.recordFailure()

            assertEquals(1, bannedIp.failureCount)
            assertFalse(bannedIp.isBanned)
        }

        @Test
        @DisplayName("5회째 실패 → 자동 차단")
        fun bansAtFiveFailures() {
            val bannedIp = BannedIp(
                ipAddress = "192.168.1.1",
                failureCount = 4,
                lastFailureAt = LocalDateTime.now().minusMinutes(10)
            )
            bannedIp.recordFailure()

            assertEquals(5, bannedIp.failureCount)
            assertTrue(bannedIp.isBanned)
        }

        @Test
        @DisplayName("4회 실패 → 아직 차단되지 않음")
        fun notBannedAtFourFailures() {
            val bannedIp = BannedIp(
                ipAddress = "192.168.1.1",
                failureCount = 3,
                lastFailureAt = LocalDateTime.now().minusMinutes(5)
            )
            bannedIp.recordFailure()

            assertEquals(4, bannedIp.failureCount)
            assertFalse(bannedIp.isBanned)
        }

        @Test
        @DisplayName("이미 차단된 상태에서 추가 실패 → 차단 유지, 카운트 증가")
        fun staysBannedOnAdditionalFailure() {
            val bannedIp = BannedIp(
                ipAddress = "192.168.1.1",
                failureCount = 5,
                isBanned = true,
                lastFailureAt = LocalDateTime.now().minusMinutes(1)
            )
            bannedIp.recordFailure()

            assertEquals(6, bannedIp.failureCount)
            assertTrue(bannedIp.isBanned)
        }

        @Test
        @DisplayName("24시간 리셋 후 다시 5회 실패하면 재차단")
        fun reBansAfterReset() {
            val bannedIp = BannedIp(
                ipAddress = "192.168.1.1",
                failureCount = 3,
                isBanned = false,
                lastFailureAt = LocalDateTime.now().minusHours(25)
            )

            bannedIp.recordFailure()
            assertEquals(1, bannedIp.failureCount)
            assertFalse(bannedIp.isBanned)

            repeat(4) { bannedIp.recordFailure() }
            assertEquals(5, bannedIp.failureCount)
            assertTrue(bannedIp.isBanned)
        }
    }
}
