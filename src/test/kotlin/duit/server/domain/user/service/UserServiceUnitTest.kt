package duit.server.domain.user.service

import com.google.firebase.auth.FirebaseToken
import duit.server.application.security.SecurityUtil
import duit.server.domain.user.dto.UpdateNicknameRequest
import duit.server.domain.user.dto.UpdateUserSettingsRequest
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import io.mockk.*
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*

@DisplayName("UserService 단위 테스트")
class UserServiceUnitTest {

    private lateinit var userRepository: UserRepository
    private lateinit var securityUtil: SecurityUtil
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        securityUtil = mockk()
        userService = UserService(userRepository, securityUtil)
    }

    private fun createUser(
        id: Long = 1L,
        nickname: String = "테스트유저",
        email: String? = "test@example.com",
        providerId: String = "provider-1",
        autoAddBookmarkToCalendar: Boolean = false,
    ) = User(
        id = id,
        nickname = nickname,
        email = email,
        providerType = ProviderType.GOOGLE,
        providerId = providerId,
        autoAddBookmarkToCalendar = autoAddBookmarkToCalendar,
    )

    @Nested
    @DisplayName("createUser - 닉네임 생성 로직")
    inner class CreateUserTests {

        @Test
        @DisplayName("기본 닉네임이 고유하면 그대로 사용한다")
        fun usesBaseNameWhenUnique() {
            val token = mockk<FirebaseToken>()
            every { token.name } returns "홍길동"
            every { token.email } returns "hong@example.com"
            every { token.uid } returns "uid-123"
            every { userRepository.existsByNickname("홍길동") } returns false
            every { userRepository.save(any<User>()) } answers { firstArg() }

            val result = userService.createUser(ProviderType.GOOGLE, token)

            assertEquals("홍길동", result.nickname)
        }

        @Test
        @DisplayName("기본 닉네임이 중복이면 카운터를 붙여 생성한다")
        fun appendsCounterWhenDuplicate() {
            val token = mockk<FirebaseToken>()
            every { token.name } returns "홍길동"
            every { token.email } returns "hong@example.com"
            every { token.uid } returns "uid-123"
            every { userRepository.existsByNickname("홍길동") } returns true
            every { userRepository.existsByNickname("홍길동1") } returns false
            every { userRepository.save(any<User>()) } answers { firstArg() }

            val result = userService.createUser(ProviderType.GOOGLE, token)

            assertEquals("홍길동1", result.nickname)
        }

        @Test
        @DisplayName("여러 번 중복이면 카운터를 증가시킨다")
        fun incrementsCounterMultipleTimes() {
            val token = mockk<FirebaseToken>()
            every { token.name } returns "유저"
            every { token.email } returns "user@example.com"
            every { token.uid } returns "uid-456"
            every { userRepository.existsByNickname("유저") } returns true
            every { userRepository.existsByNickname("유저1") } returns true
            every { userRepository.existsByNickname("유저2") } returns true
            every { userRepository.existsByNickname("유저3") } returns false
            every { userRepository.save(any<User>()) } answers { firstArg() }

            val result = userService.createUser(ProviderType.GOOGLE, token)

            assertEquals("유저3", result.nickname)
        }

        @Test
        @DisplayName("name이 null이면 email 앞부분을 사용한다")
        fun usesEmailPrefixWhenNameNull() {
            val token = mockk<FirebaseToken>()
            every { token.name } returns null
            every { token.email } returns "myemail@example.com"
            every { token.uid } returns "uid-789"
            every { userRepository.existsByNickname("myemail") } returns false
            every { userRepository.save(any<User>()) } answers { firstArg() }

            val result = userService.createUser(ProviderType.GOOGLE, token)

            assertEquals("myemail", result.nickname)
        }

        @Test
        @DisplayName("name과 email 모두 null이면 '사용자'를 사용한다")
        fun usesDefaultWhenBothNull() {
            val token = mockk<FirebaseToken>()
            every { token.name } returns null
            every { token.email } returns null
            every { token.uid } returns "uid-000"
            every { userRepository.existsByNickname("사용자") } returns false
            every { userRepository.save(any<User>()) } answers { firstArg() }

            val result = userService.createUser(ProviderType.GOOGLE, token)

            assertEquals("사용자", result.nickname)
            assertEquals("", result.email)
        }
    }

    @Nested
    @DisplayName("checkNicknameAvailable")
    inner class CheckNicknameAvailableTests {

        @Test
        @DisplayName("사용 가능한 닉네임이면 예외가 발생하지 않는다")
        fun availableNickname() {
            every { userRepository.existsByNickname("새닉네임") } returns false

            assertDoesNotThrow { userService.checkNicknameAvailable("새닉네임") }
        }

        @Test
        @DisplayName("이미 존재하는 닉네임이면 IllegalArgumentException이 발생한다")
        fun duplicateNickname() {
            every { userRepository.existsByNickname("중복닉네임") } returns true

            val exception = assertThrows<IllegalArgumentException> {
                userService.checkNicknameAvailable("중복닉네임")
            }
            assertTrue(exception.message!!.contains("중복닉네임"))
        }
    }

    @Nested
    @DisplayName("updateCurrentUserNickname")
    inner class UpdateNicknameTests {

        @Test
        @DisplayName("현재 닉네임과 같은 닉네임으로 수정하면 중복 체크 없이 성공한다")
        fun sameNicknameSkipsDuplicateCheck() {
            val user = createUser(nickname = "기존닉네임")
            every { securityUtil.getCurrentUserId() } returns 1L
            every { userRepository.findById(1L) } returns Optional.of(user)

            val result = userService.updateCurrentUserNickname(UpdateNicknameRequest("기존닉네임"))

            assertEquals("기존닉네임", result.nickname)
            verify(exactly = 0) { userRepository.existsByNickname(any()) }
        }

        @Test
        @DisplayName("다른 닉네임으로 수정 시 사용 가능하면 성공한다")
        fun differentNicknameAvailable() {
            val user = createUser(nickname = "기존닉네임")
            every { securityUtil.getCurrentUserId() } returns 1L
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userRepository.existsByNickname("새닉네임") } returns false

            val result = userService.updateCurrentUserNickname(UpdateNicknameRequest("새닉네임"))

            assertEquals("새닉네임", result.nickname)
        }

        @Test
        @DisplayName("다른 닉네임으로 수정 시 이미 사용 중이면 IllegalArgumentException이 발생한다")
        fun differentNicknameTaken() {
            val user = createUser(nickname = "기존닉네임")
            every { securityUtil.getCurrentUserId() } returns 1L
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userRepository.existsByNickname("사용중닉네임") } returns true

            assertThrows<IllegalArgumentException> {
                userService.updateCurrentUserNickname(UpdateNicknameRequest("사용중닉네임"))
            }
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    inner class GetCurrentUserTests {

        @Test
        @DisplayName("현재 사용자 정보를 반환한다")
        fun returnsCurrentUser() {
            val user = createUser()
            every { securityUtil.getCurrentUserId() } returns 1L
            every { userRepository.findById(1L) } returns Optional.of(user)

            val result = userService.getCurrentUser()

            assertEquals(1L, result.id)
            assertEquals("테스트유저", result.nickname)
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 EntityNotFoundException이 발생한다")
        fun throwsWhenNotFound() {
            every { securityUtil.getCurrentUserId() } returns 999L
            every { userRepository.findById(999L) } returns Optional.empty()

            assertThrows<EntityNotFoundException> {
                userService.getCurrentUser()
            }
        }
    }

    @Nested
    @DisplayName("updateDevice")
    inner class UpdateDeviceTests {

        @Test
        @DisplayName("디바이스 토큰을 정상적으로 업데이트한다")
        fun updatesDeviceToken() {
            val user = createUser()
            every { securityUtil.getCurrentUserId() } returns 1L
            every { userRepository.findById(1L) } returns Optional.of(user)

            userService.updateDevice("new-fcm-token")

            assertEquals("new-fcm-token", user.deviceToken)
        }
    }

    @Nested
    @DisplayName("updateUserSettings")
    inner class UpdateSettingsTests {

        @Test
        @DisplayName("알림 설정을 정상적으로 업데이트한다")
        fun updatesSettings() {
            val user = createUser()
            every { securityUtil.getCurrentUserId() } returns 1L
            every { userRepository.findById(1L) } returns Optional.of(user)

            val request = UpdateUserSettingsRequest(
                autoAddBookmarkToCalendar = true,
                alarmSettings = UpdateUserSettingsRequest.AlarmSettingsDto(
                    push = false, bookmark = true, calendar = false, marketing = false
                )
            )

            val result = userService.updateUserSettings(request)

            assertTrue(result.autoAddBookmarkToCalendar)
            assertEquals(false, result.alarmSettings.push)
            assertEquals(true, result.alarmSettings.bookmark)
        }
    }

    @Nested
    @DisplayName("withdraw")
    inner class WithdrawTests {

        @Test
        @DisplayName("현재 사용자를 삭제한다")
        fun deletesCurrentUser() {
            every { securityUtil.getCurrentUserId() } returns 1L
            every { userRepository.deleteById(1L) } just runs

            userService.withdraw()

            verify(exactly = 1) { userRepository.deleteById(1L) }
        }
    }
}
