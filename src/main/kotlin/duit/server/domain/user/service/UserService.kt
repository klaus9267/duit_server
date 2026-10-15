package duit.server.domain.user.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.job.repository.CompanyBookmarkRepository
import duit.server.domain.job.repository.JobBookmarkRepository
import duit.server.domain.subscription.repository.SubscriptionRepository
import duit.server.domain.user.dto.UpdateNicknameRequest
import duit.server.domain.user.dto.UpdateUserSettingsRequest
import duit.server.domain.user.dto.UserPaginationParam
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserDeviceTokenRepository
import duit.server.domain.user.repository.UserRepository
import duit.server.infrastructure.external.firebase.FirebaseUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val userDeviceTokenRepository: UserDeviceTokenRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val jobBookmarkRepository: JobBookmarkRepository,
    private val companyBookmarkRepository: CompanyBookmarkRepository,
    private val firebaseUtil: FirebaseUtil,
    private val securityUtil: SecurityUtil
) {

    @Transactional
    fun createUser(providerType: ProviderType, uid: String, email: String?, name: String?): User {
        val newUser = User(
            email = email ?: "",
            nickname = generateNickname(
                name ?: email?.substringBefore("@") ?: "사용자"
            ),
            providerType = providerType,
            providerId = uid
        )
        return userRepository.save(newUser)
    }

    private fun generateNickname(baseName: String): String {
        var nickname = baseName
        var counter = 1

        while (userRepository.existsByNickname(nickname)) {
            nickname = "${baseName}${counter}"
            counter++
        }

        return nickname
    }

    /**
     * 사용자 목록 조회 (관리자용)
     */
    fun getAllUsers(param: UserPaginationParam): PageResponse<UserResponse> {
        val pageable = param.toPageable()
        val page = userRepository.findAll(pageable)

        return PageResponse(
            content = page.map { UserResponse.from(it) }.toList(),
            pageInfo = PageInfo.from(page)
        )
    }

    /**
     * 현재 사용자 조회
     */
    fun getCurrentUser(): UserResponse {
        val currentUserId = securityUtil.getCurrentUserId()
        val user = findUserById(currentUserId)
        return UserResponse.from(user)
    }

    /**
     * 특정 사용자 조회 (관리자용 - 필요시 권한 체크 추가)
     */
    fun getUser(userId: Long): UserResponse {
        val user = findUserById(userId)
        return UserResponse.from(user)
    }

    /**
     * 현재 사용자 닉네임 수정
     */
    @Transactional
    fun updateCurrentUserNickname(request: UpdateNicknameRequest): UserResponse {
        val currentUserId = securityUtil.getCurrentUserId()
        val user = findUserById(currentUserId)

        if (user.nickname != request.nickname && userRepository.existsByNickname(request.nickname)) {
            throw IllegalArgumentException("이미 사용 중인 닉네임입니다: ${request.nickname}")
        }

        user.updateNickname(request.nickname)
        return UserResponse.from(user)
    }

    @Transactional
    fun registerDeviceToken(token: String) {
        val currentUserId = securityUtil.getCurrentUserId()
        val user = findUserById(currentUserId)
        val existingTokens = userDeviceTokenRepository.findAllByToken(token)

        when {
            existingTokens.any { it.user.id != currentUserId } ->
                throw IllegalStateException("이미 다른 사용자에 등록된 디바이스 토큰입니다")

            else -> {
                user.deviceToken = token
                user.registerDeviceToken(token)
            }
        }
    }


    @Transactional
    fun deleteDeviceToken(token: String) {
        val user = findUserById(securityUtil.getCurrentUserId())
        user.unregisterDeviceToken(token)

        if (user.deviceToken == token) {
            user.deviceToken = user.deviceTokens.lastOrNull()?.token
        }
    }

    /**
     * 현재 사용자 설정 통합 수정 (알림 + 캘린더)
     */
    @Transactional
    fun updateUserSettings(request: UpdateUserSettingsRequest): UserResponse {
        val currentUserId = securityUtil.getCurrentUserId()
        val user = findUserById(currentUserId)
        user.updateSettings(
            newAlarmSettings = request.alarmSettings.toAlarmSettings(),
            autoAddBookmarkToCalendar = request.autoAddBookmarkToCalendar
        )
        return UserResponse.from(user)
    }

    /**
     * 회원탈퇴
     * - User 에 매핑되지 않은 구독·채용공고 북마크·회사 북마크를 먼저 지운다 (FK 위반 방지)
     * - 행사 북마크·알림·디바이스 토큰·관리자 계정은 User cascade 로 함께 삭제된다
     * - 커밋으로 DB 삭제가 확정된 뒤 Firebase Auth 계정도 지운다 (실패해도 탈퇴는 완료)
     */
    @Transactional
    fun withdraw() {
        val user = findUserById(securityUtil.getCurrentUserId())
        val userId = user.id!!

        subscriptionRepository.deleteByUserId(userId)
        jobBookmarkRepository.deleteByUserId(userId)
        companyBookmarkRepository.deleteByUserId(userId)
        userRepository.delete(user)
        userRepository.flush()

        user.providerId?.let { uid -> afterCommit { firebaseUtil.deleteUserQuietly(uid) } }
    }

    // 커밋이 실패하면 DB 회원은 남는데 Firebase 계정만 지워지는 일을 막는다
    private fun afterCommit(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return action()
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = action()
        })
    }

    /**
     * 닉네임 사용 가능 여부 확인
     */
    fun checkNicknameAvailable(nickname: String) {
        if (userRepository.existsByNickname(nickname)) {
            throw IllegalArgumentException("이미 사용 중인 닉네임입니다: $nickname")
        }
    }

    fun findUserById(userId: Long): User {
        return userRepository.findByIdOrThrow(userId)
    }
}
