package duit.server.domain.user.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.user.dto.UpdateNicknameRequest
import duit.server.domain.user.dto.UpdateUserSettingsRequest
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val securityUtil: SecurityUtil
) {

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
    fun updateDevice(token: String) {
        val currentUserId = securityUtil.getCurrentUserId()
        val user = findUserById(currentUserId)
        user.deviceToken = token
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

        val user = findUserById(currentUserId)
        userRepository.delete(user)
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
        return userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("사용자를 찾을 수 없습니다: $userId") }
    }
}
