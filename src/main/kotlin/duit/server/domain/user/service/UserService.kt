package duit.server.domain.user.service

import duit.server.application.controller.dto.user.UpdateNicknameRequest
import duit.server.application.controller.dto.user.UserResponse
import duit.server.application.security.SecurityUtil
import duit.server.domain.user.entity.User
import duit.server.domain.user.exception.DuplicateNicknameException
import duit.server.domain.user.exception.UserNotFoundException
import duit.server.domain.user.repository.UserRepository
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

        // 닉네임 중복 검증 (본인 제외)
        if (user.nickname != request.nickname && userRepository.existsByNickname(request.nickname)) {
            throw DuplicateNicknameException(request.nickname)
        }

        user.updateNickname(request.nickname)
        return UserResponse.from(user)
    }

    /**
     * 특정 사용자 닉네임 수정 (관리자용 - 필요시 권한 체크 추가)
     */
    @Transactional
    fun updateNickname(request: UpdateNicknameRequest): UserResponse {
        val currentUserId = securityUtil.getCurrentUserId()
        val user = findUserById(currentUserId)

        // 닉네임 중복 검증 (본인 제외)
        if (user.nickname != request.nickname && userRepository.existsByNickname(request.nickname)) {
            throw DuplicateNicknameException(request.nickname)
        }

        user.updateNickname(request.nickname)
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
            throw DuplicateNicknameException(nickname)
        }
    }

    /**
     * JWT 토큰 생성을 위한 사용자 인증 (로그인 시 사용)
     * 실제 로그인 로직은 별도 AuthService에서 구현하는 것을 권장
     */
    fun findUserForAuthentication(userId: Long): User {
        return findUserById(userId)
    }

// Private 메서드들

    private fun findUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }
    }
}
