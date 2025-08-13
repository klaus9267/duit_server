package duit.server.domain.auth.service

import com.google.firebase.auth.FirebaseAuth
import duit.server.application.security.JwtTokenProvider
import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val firebaseAuth: FirebaseAuth,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository
) {

    @Transactional
    fun socialLogin(idToken: String): AuthResponse {
        val token = firebaseAuth.verifyIdToken(idToken)
        val existingUser = userRepository.findByProviderId(token.uid)
        val user = existingUser ?: run {
            val newUser = User(
                email = token.email,
                nickname = generateNickname(
                    token.name ?: token.email?.substringBefore("@") ?: "사용자"
                ),
                providerType = ProviderType.KAKAO,
                providerId = token.uid
            )
            userRepository.save(newUser)
        }

        val accessToken = jwtTokenProvider.createAccessToken(user.id!!)

        return AuthResponse(
            accessToken = accessToken,
            user = UserResponse.from(user),
            isNewUser = existingUser == null
        )
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
}