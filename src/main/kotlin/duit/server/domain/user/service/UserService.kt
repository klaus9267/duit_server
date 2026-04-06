package duit.server.domain.user.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.user.dto.UpdateNicknameRequest
import duit.server.domain.user.dto.UpdateUserSettingsRequest
import duit.server.domain.user.dto.UserPaginationParam
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.user.entity.UserDeviceToken
import duit.server.domain.user.repository.UserDeviceTokenRepository
import duit.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val userDeviceTokenRepository: UserDeviceTokenRepository,
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

        userDeviceTokenRepository.findByToken(token)?.let { existingToken ->
            if (existingToken.user.id == currentUserId) {
                user.deviceToken = token
                return
            }
            throw IllegalStateException("이미 다른 사용자에 등록된 디바이스 토큰입니다")
        }

        user.registerDeviceToken(token)
        user.deviceToken = token
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
     * 회원탈퇴 (기존 메서드 - 하드코딩 제거)
     */
    @Transactional
    fun withdraw() {
        val currentUserId = securityUtil.getCurrentUserId()
        userRepository.deleteById(currentUserId)
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
