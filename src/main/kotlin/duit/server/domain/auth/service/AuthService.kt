package duit.server.domain.auth.service

import duit.server.application.security.JwtTokenProvider
import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.auth.dto.FirebaseTokenClaims
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.repository.UserRepository
import duit.server.domain.user.service.UserService
import duit.server.infrastructure.external.firebase.FirebaseTokenVerifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val firebaseTokenVerifier: FirebaseTokenVerifier,
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

        val firebaseStart = System.currentTimeMillis()
        val token = firebaseTokenVerifier.verifyIdToken(trimmedToken)
        val firebaseElapsed = System.currentTimeMillis() - firebaseStart

        val dbStart = System.currentTimeMillis()
        val existingUser = userRepository.findByProviderId(token.uid)
        val dbElapsed = System.currentTimeMillis() - dbStart

        var createUserElapsed = 0L
        val user = existingUser ?: run {
            val createStart = System.currentTimeMillis()
            val providerType = determineProviderType(token)
            val newUser = userService.createUser(providerType, token.uid, token.email, token.name)
            createUserElapsed = System.currentTimeMillis() - createStart
            newUser
        }

        val jwtStart = System.currentTimeMillis()
        val accessToken = jwtTokenProvider.createAccessToken(user.id!!)
        val jwtElapsed = System.currentTimeMillis() - jwtStart

        val totalElapsed = System.currentTimeMillis() - totalStart

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

    private fun determineProviderType(token: FirebaseTokenClaims): ProviderType {
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