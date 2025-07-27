package duit.server.domain.user.service

import duit.server.application.controller.dto.user.UpdateNicknameRequest
import duit.server.application.controller.dto.user.UserResponse
import duit.server.domain.user.entity.User
import duit.server.domain.user.exception.DuplicateNicknameException
import duit.server.domain.user.exception.UserNotFoundException
import duit.server.domain.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * 사용자 조회
     */
    fun getUser(userId: Long): UserResponse {
        val user = findUserById(userId)
        return UserResponse.from(user)
    }

    /**
     * 닉네임 수정
     */
    @Transactional
    fun updateNickname(userId: Long, request: UpdateNicknameRequest): UserResponse {
        val user = findUserById(userId)

        // 닉네임 중복 검증 (본인 제외)
        if (user.nickname != request.nickname && userRepository.existsByNickname(request.nickname)) {
            throw DuplicateNicknameException(request.nickname)
        }

        user.updateNickname(request.nickname)
        return UserResponse.from(user)
    }

    /**
     * 회원탈퇴
     */
    @Transactional
    fun withdraw() {
        val user = findUserById(1L)
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


// Private 메서드들

    private fun findUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }
    }
}
