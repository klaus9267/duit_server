package duit.server.domain.user.service

import duit.server.application.dto.user.LoginRequest
import duit.server.application.dto.user.UpdateNicknameRequest
import duit.server.application.dto.user.UserRequest
import duit.server.application.dto.user.UserResponse
import duit.server.domain.user.entity.User
import duit.server.domain.user.exception.*
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
     * 회원가입
     */
    @Transactional
    fun join(request: UserRequest): UserResponse {
        // 중복 검증
        validateDuplicateLoginId(request.loginId)
        validateDuplicateNickname(request.nickname)
        
        // 비밀번호 암호화
        val hashedPassword = passwordEncoder.encode(request.password)
        
        // 사용자 생성 및 저장
        val user = request.toEntity(hashedPassword)
        val savedUser = userRepository.save(user)
        
        return UserResponse.from(savedUser)
    }
    
    /**
     * 로그인
     */
    fun login(request: LoginRequest): UserResponse {
        val user = userRepository.findByLoginId(request.loginId)
            .orElseThrow { UserLoginIdNotFoundException(request.loginId) }
        
        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidPasswordException()
        }
        
        return UserResponse.from(user)
    }
    
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
        if (user.nickname != request.nickname) {
            validateDuplicateNickname(request.nickname)
        }
        
        user.updateNickname(request.nickname)
        return UserResponse.from(user)
    }
    
    /**
     * 회원탈퇴
     */
    @Transactional
    fun withdraw(userId: Long) {
        val user = findUserById(userId)
        userRepository.delete(user)
    }
    
    /**
     * 로그인 ID 사용 가능 여부 확인
     */
    fun checkLoginIdAvailable(loginId: String): Boolean {
        return !userRepository.existsByLoginId(loginId)
    }
    
    /**
     * 닉네임 사용 가능 여부 확인
     */
    fun checkNicknameAvailable(nickname: String): Boolean {
        return !userRepository.existsByNickname(nickname)
    }
    
    // Private 메서드들
    
    private fun findUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }
    }
    
    private fun validateDuplicateLoginId(loginId: String) {
        if (userRepository.existsByLoginId(loginId)) {
            throw DuplicateLoginIdException(loginId)
        }
    }
    
    private fun validateDuplicateNickname(nickname: String) {
        if (userRepository.existsByNickname(nickname)) {
            throw DuplicateNicknameException(nickname)
        }
    }
}
