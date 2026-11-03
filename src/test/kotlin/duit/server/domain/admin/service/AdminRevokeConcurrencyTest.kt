package duit.server.domain.admin.service

import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.user.repository.UserRepository
import duit.server.support.TestFirebaseConfig
import duit.server.support.TestRedisConfig
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(TestRedisConfig::class, TestFirebaseConfig::class)
@ActiveProfiles("test")
@DisplayName("관리자 해제 동시성 테스트")
class AdminRevokeConcurrencyTest {

    @Autowired
    private lateinit var adminService: AdminService

    @Autowired
    private lateinit var adminRepository: AdminRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private val createdUserIds = mutableListOf<Long>()

    @AfterEach
    fun cleanUp() {
        userRepository.deleteAllById(createdUserIds)
        createdUserIds.clear()
    }

    private fun createAdmin(suffix: String): Admin {
        val user = userRepository.save(
            TestFixtures.user(nickname = "동시성-$suffix", providerId = "concurrency-$suffix", email = null)
        ).also { createdUserIds.add(it.id!!) }
        return adminRepository.save(TestFixtures.admin(user = user, adminId = "concurrency-$suffix"))
    }

    @RepeatedTest(10)
    @DisplayName("관리자 둘이 서로를 동시에 해제해도 한 명은 반드시 남는다")
    fun mutualRevokeKeepsOneAdmin() {
        val suffix = System.nanoTime().toString()
        val first = createAdmin("a-$suffix")
        val second = createAdmin("b-$suffix")
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val results = listOf(first to second, second to first).map { (caller, target) ->
                executor.submit<Result<Unit>> {
                    SecurityContextHolder.getContext().authentication =
                        UsernamePasswordAuthenticationToken(caller.user.id, null, emptyList())
                    try {
                        barrier.await(5, TimeUnit.SECONDS)
                        runCatching { adminService.revokeAdmin(target.id!!) }
                    } finally {
                        SecurityContextHolder.clearContext()
                    }
                }
            }.map { it.get(15, TimeUnit.SECONDS) }

            assertEquals(1, results.count { it.isSuccess }, "정확히 한 쪽만 성공해야 합니다: $results")
            assertEquals(
                1, results.count { it.exceptionOrNull() is AccessDeniedException },
                "실패한 쪽은 이미 권한을 잃은 요청자로 거부되어야 합니다: $results"
            )
            assertEquals(1, listOf(first, second).count { adminRepository.existsById(it.id!!) }, "관리자가 한 명 남아야 합니다")
        } finally {
            executor.shutdownNow()
        }
    }
}
