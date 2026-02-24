package duit.server.infrastructure.external.firebase

import duit.server.domain.auth.dto.FirebaseTokenClaims
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtValidationException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.stereotype.Component

@Component
class FirebaseTokenVerifier(
    @Value("\${firebase.project-id}")
    private val projectId: String,
    private val jwksUri: String = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
) {
    private var customDecoder: JwtDecoder? = null

    private val jwtDecoder: JwtDecoder by lazy {
        customDecoder ?: createDefaultDecoder()
    }

    private fun createDefaultDecoder(): JwtDecoder {
        val decoder = NimbusJwtDecoder
            .withJwkSetUri(jwksUri)
            .build()

        val validators = listOf(
            JwtTimestampValidator(),
            JwtIssuerValidator("https://securetoken.google.com/$projectId"),
            JwtClaimValidator<List<String>>("aud") { it.contains(projectId) }
        )
        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(validators))
        return decoder
    }

    fun setDecoder(decoder: JwtDecoder) {
        this.customDecoder = decoder
    }

    fun verifyIdToken(idToken: String): FirebaseTokenClaims {
        return try {
            val jwt = jwtDecoder.decode(idToken)
            FirebaseTokenClaims(
                uid = jwt.subject,
                email = jwt.getClaimAsString("email"),
                name = jwt.getClaimAsString("name"),
                claims = jwt.claims
            )
        } catch (e: JwtValidationException) {
            throw IllegalArgumentException("Firebase 토큰 검증 실패: ${e.message}", e)
        } catch (e: JwtException) {
            throw IllegalArgumentException("유효하지 않은 토큰 형식입니다. 올바른 Firebase ID 토큰을 전달해주세요.", e)
        }
    }
}
