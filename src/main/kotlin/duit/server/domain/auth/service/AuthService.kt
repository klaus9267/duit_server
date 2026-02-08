package duit.server.domain.auth.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import duit.server.application.security.JwtTokenProvider
import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.repository.UserRepository
import duit.server.domain.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val firebaseAuth: FirebaseAuth,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun socialLogin(idToken: String): AuthResponse {
        val totalStart = System.currentTimeMillis()
        val trimmedToken = idToken.trim()

        require(trimmedToken.isNotBlank() && trimmedToken != "null") {
            "ID 토큰이 비어있습니다. 올바른 Firebase ID 토큰을 전달해주세요."
        }

        // Firebase 토큰 검증
        val firebaseStart = System.currentTimeMillis()
        val token = try {
            firebaseAuth.verifyIdToken(trimmedToken)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("유효하지 않은 토큰 형식입니다.($trimmedToken) 올바른 Firebase ID 토큰을 전달해주세요.", e)
        } catch (e: FirebaseAuthException) {
            throw IllegalArgumentException("Firebase 토큰 검증 실패($trimmedToken): ${e.message}", e)
        }
        val firebaseElapsed = System.currentTimeMillis() - firebaseStart

        // DB 조회
        val dbStart = System.currentTimeMillis()
        val existingUser = userRepository.findByProviderId(token.uid)
        val dbElapsed = System.currentTimeMillis() - dbStart

        // 사용자 생성 (신규 사용자)
        var createUserElapsed = 0L
        val user = existingUser ?: run {
            val createStart = System.currentTimeMillis()
            val providerType = determineProviderType(token)
            val newUser = userService.createUser(providerType, token)
            createUserElapsed = System.currentTimeMillis() - createStart
            newUser
        }

        // JWT 생성
        val jwtStart = System.currentTimeMillis()
        val accessToken = jwtTokenProvider.createAccessToken(user.id!!)
        val jwtElapsed = System.currentTimeMillis() - jwtStart

        val totalElapsed = System.currentTimeMillis() - totalStart

        // 500ms 이상일 때만 로깅
        if (totalElapsed > 500) {
            logger.warn(
                "Slow socialLogin: total={}ms, firebase={}ms, db={}ms, createUser={}ms, jwt={}ms, isNewUser={}",
                totalElapsed, firebaseElapsed, dbElapsed, createUserElapsed, jwtElapsed, existingUser == null
            )
        }

        return AuthResponse(
            accessToken = accessToken,
            user = UserResponse.from(user),
            isNewUser = existingUser == null
        )
    }

    /**
     * Firebase 토큰에서 프로바이더 타입을 결정합니다.
     */
    private fun determineProviderType(token: FirebaseToken): ProviderType {
        val providerId = token.claims["firebase"]?.let {
            (it as? Map<*, *>)?.get("sign_in_provider") as? String
        }

        return when (providerId) {
            "google.com" -> ProviderType.GOOGLE
            "apple.com" -> ProviderType.APPLE
            "oidc.kakao" -> ProviderType.KAKAO
            "oidc.kakao_rest" -> ProviderType.KAKAO
            else -> {
                throw RuntimeException("잘못된 소셜로그인 사용 정보 입니다.")
            }
        }
    }
}