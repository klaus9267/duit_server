package duit.server.domain.admin.service

import com.fasterxml.jackson.databind.ObjectMapper
import duit.server.application.security.JwtTokenProvider
import duit.server.domain.admin.dto.AdminRegisterRequest
import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import duit.server.support.TestFirebaseConfig
import duit.server.support.TestRedisConfig
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 운영 DB 의 admins 에 unique 제약이 없고 중복 행이 있어도 관리자 관리 기능이 정상 동작하는지 검증한다.
 * 다른 테스트와 스키마를 공유하지 않도록 별도의 H2 DB 를 쓰고, 그 안에서 unique 제약을 제거한다.
 */
@SpringBootTest
@Import(TestRedisConfig::class, TestFirebaseConfig::class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.datasource.url=jdbc:h2:mem:legacy-admins;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL"])
@DisplayName("admins 에 unique 제약이 없는 DB 에서의 관리자 관리 테스트")
class AdminLegacySchemaTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var adminService: AdminService

    @Autowired
    private lateinit var adminRepository: AdminRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun dropUniqueConstraints() {
        fun constraintNames(type: String) = jdbcTemplate.queryForList(
            "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE TABLE_NAME = 'ADMINS' AND CONSTRAINT_TYPE = ?",
            String::class.java, type
        )

        val uniques = constraintNames("UNIQUE")
        if (uniques.isEmpty()) return

        // FK 가 unique 인덱스를 재사용하므로 FK 를 먼저 지운 뒤 unique 를 지우고 FK 를 다시 만든다
        constraintNames("FOREIGN KEY").forEach { jdbcTemplate.execute("ALTER TABLE admins DROP CONSTRAINT \"$it\"") }
        uniques.forEach { jdbcTemplate.execute("ALTER TABLE admins DROP CONSTRAINT \"$it\"") }
        jdbcTemplate.execute("ALTER TABLE admins ADD CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users (id)")
    }

    @AfterEach
    fun cleanUp() {
        jdbcTemplate.update("DELETE FROM admins")
        jdbcTemplate.update("DELETE FROM users")
        SecurityContextHolder.clearContext()
    }

    private fun user(name: String): User =
        userRepository.save(TestFixtures.user(nickname = name, providerId = "legacy-$name", email = "$name@example.com"))

    private fun admin(user: User, adminId: String, password: String = "pw"): Admin =
        adminRepository.save(TestFixtures.admin(user = user, adminId = adminId, password = password))

    private fun rowsOf(user: User): Long =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admins WHERE user_id = ?", Long::class.java, user.id)!!

    private fun auth(user: User) = "Bearer ${jwtTokenProvider.createAccessToken(user.id!!)}"

    private fun listAdmins(caller: User): ResultActions =
        mockMvc.perform(get("/api/v1/admins").header("Authorization", auth(caller)))

    private fun revoke(caller: User, adminPk: Long): ResultActions =
        mockMvc.perform(delete("/api/v1/admins/{id}", adminPk).header("Authorization", auth(caller)))

    private fun register(caller: User, targetUserId: Long, adminId: String): ResultActions =
        mockMvc.perform(
            post("/api/v1/admin/auth/register")
                .header("Authorization", auth(caller))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("userId" to targetUserId, "adminId" to adminId, "password" to "password-1234")
                    )
                )
        )

    private fun runConcurrently(vararg calls: Pair<User, () -> Unit>): List<Result<Unit>> {
        val barrier = CyclicBarrier(calls.size)
        val executor = Executors.newFixedThreadPool(calls.size)
        return try {
            calls.map { (caller, action) ->
                executor.submit<Result<Unit>> {
                    SecurityContextHolder.getContext().authentication =
                        UsernamePasswordAuthenticationToken(caller.id, null, emptyList())
                    try {
                        barrier.await(5, TimeUnit.SECONDS)
                        runCatching(action)
                    } finally {
                        SecurityContextHolder.clearContext()
                    }
                }
            }.map { it.get(15, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    @DisplayName("테스트 전제: 같은 사용자·같은 adminId 의 행을 중복으로 저장할 수 있다")
    fun schemaAllowsDuplicates() {
        val someone = user("someone")

        admin(someone, "same-id")
        admin(someone, "same-id")

        assertEquals(2L, rowsOf(someone))
    }

    @Nested
    @DisplayName("중복 행이 이미 있는 경우")
    inner class WithExistingDuplicates {

        @Test
        @DisplayName("목록은 중복 행을 포함해 500 없이 조회된다")
        fun listsDuplicateRows() {
            val caller = user("caller").also { admin(it, "caller-admin") }
            val target = user("target").also { admin(it, "target-1"); admin(it, "target-2") }

            listAdmins(caller)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.pageInfo.totalElements").value(3))
            assertEquals(2L, rowsOf(target))
        }

        @Test
        @DisplayName("대상 사용자의 관리자 행이 여러 개여도 해제하면 모두 삭제되고 요청자는 유지된다")
        fun revokeDeletesAllRowsOfTarget() {
            val caller = user("caller").also { admin(it, "caller-admin") }
            val target = user("target")
            val first = admin(target, "target-1")
            admin(target, "target-2")

            revoke(caller, first.id!!).andExpect(status().isNoContent)

            assertEquals(0L, rowsOf(target))
            assertEquals(1L, rowsOf(caller))
            assertTrue(userRepository.existsById(target.id!!))
        }

        @Test
        @DisplayName("본인 사용자의 관리자 행이 여러 개여도 어느 행으로든 본인은 해제할 수 없다")
        fun cannotRevokeSelfThroughAnyDuplicateRow() {
            val caller = user("caller")
            admin(caller, "caller-1")
            val second = admin(caller, "caller-2")
            val other = user("other").also { admin(it, "other-admin") }

            revoke(caller, second.id!!).andExpect(status().isBadRequest)

            assertEquals(2L, rowsOf(caller))
            assertEquals(1L, rowsOf(other))
        }

        @Test
        @DisplayName("이미 관리자 행이 중복된 사용자를 등록하려 하면 409를 반환한다")
        fun registerForUserWithDuplicateRows() {
            val caller = user("caller").also { admin(it, "caller-admin") }
            val target = user("target").also { admin(it, "target-1"); admin(it, "target-2") }

            register(caller, target.id!!, "new-admin-id").andExpect(status().isConflict)

            assertEquals(2L, rowsOf(target))
        }

        @Test
        @DisplayName("같은 adminId 의 행이 둘 이상이면 비밀번호가 맞아도 로그인은 400으로 거부된다 (500 아님)")
        fun loginRejectedForAmbiguousAdminId() {
            val password = passwordEncoder.encode("password-1234")
            admin(user("first"), "same-id", password)
            admin(user("second"), "same-id", password)

            mockMvc.perform(
                post("/api/v1/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("adminId" to "same-id", "password" to "password-1234")))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.accessToken").doesNotExist())
        }
    }

    @Nested
    @DisplayName("동시 요청")
    inner class Concurrent {

        @RepeatedTest(10)
        @DisplayName("관리자 둘이 같은 사용자를 동시에 등록해도 관리자 행은 하나만 생긴다")
        fun concurrentRegisterOfSameUserCreatesOneRow() {
            val first = user("first").also { admin(it, "first-admin") }
            val second = user("second").also { admin(it, "second-admin") }
            val target = user("target")

            val results = runConcurrently(
                first to { adminService.register(AdminRegisterRequest(target.id!!, "id-by-first", "password-1234")) },
                second to { adminService.register(AdminRegisterRequest(target.id!!, "id-by-second", "password-1234")) },
            )

            assertEquals(1, results.count { it.isSuccess }, "정확히 한 쪽만 성공해야 합니다: $results")
            assertEquals(1, results.count { it.exceptionOrNull() is IllegalStateException }, "나머지는 409(IllegalStateException)여야 합니다: $results")
            assertEquals(1L, rowsOf(target))
        }

        @RepeatedTest(10)
        @DisplayName("관리자 둘이 서로 다른 사용자에게 같은 adminId 를 동시에 발급해도 하나만 생긴다")
        fun concurrentRegisterOfSameAdminIdCreatesOneRow() {
            val first = user("first").also { admin(it, "first-admin") }
            val second = user("second").also { admin(it, "second-admin") }
            val targetOfFirst = user("target-1")
            val targetOfSecond = user("target-2")

            val results = runConcurrently(
                first to { adminService.register(AdminRegisterRequest(targetOfFirst.id!!, "contested-id", "password-1234")) },
                second to { adminService.register(AdminRegisterRequest(targetOfSecond.id!!, "contested-id", "password-1234")) },
            )

            assertEquals(1, results.count { it.isSuccess }, "정확히 한 쪽만 성공해야 합니다: $results")
            assertEquals(1, results.count { it.exceptionOrNull() is IllegalArgumentException }, "나머지는 400(IllegalArgumentException)이어야 합니다: $results")
            assertEquals(1L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admins WHERE admin_id = 'contested-id'", Long::class.java))
        }
    }
}
