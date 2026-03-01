package duit.server.infrastructure.external.firebase

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import duit.server.domain.auth.dto.FirebaseTokenClaims
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.JwtValidationException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Component
class FirebaseTokenVerifier(
    @Value("\${firebase.project-id}")
    private val projectId: String,
    private val jwksUri: String = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
) {
    private val logger = LoggerFactory.getLogger(FirebaseTokenVerifier::class.java)

    private val fetcher: RestTemplate = RestTemplateBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(10))
        .build()

    @Volatile
    private var cachedDecoder: JwtDecoder? = null

    private val activeDecoder: JwtDecoder
        get() = cachedDecoder ?: error("JWKS decoder not initialized. Call setDecoder() or ensure @PostConstruct ran.")

    @PostConstruct
    fun init() {
        if (cachedDecoder == null) {
            cachedDecoder = buildDecoder(fetchJwks())
            logger.info("JWKS pre-warmed at startup")
        }
    }

    @Scheduled(fixedRate = 4 * 60 * 60 * 1000)
    fun refreshJwks() {
        runCatching { buildDecoder(fetchJwks()) }
            .onSuccess { cachedDecoder = it; logger.info("JWKS cache refreshed successfully") }
            .onFailure { logger.warn("JWKS refresh failed, using existing cache: ${it.message}") }
    }

    private fun fetchJwks(): JWKSet {
        val json = fetcher.getForObject(jwksUri, String::class.java)
            ?: throw IllegalStateException("Failed to fetch JWKS from $jwksUri")
        return JWKSet.parse(json)
    }

    private fun buildDecoder(jwkSet: JWKSet): NimbusJwtDecoder {
        val processor: ConfigurableJWTProcessor<SecurityContext> = DefaultJWTProcessor()
        processor.jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, ImmutableJWKSet(jwkSet))
        val decoder = NimbusJwtDecoder(processor)
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                listOf(
                    JwtTimestampValidator(),
                    JwtIssuerValidator("https://securetoken.google.com/$projectId"),
                    JwtClaimValidator<List<String>>("aud") { it.contains(projectId) }
                )
            )
        )
        return decoder
    }

    fun setDecoder(decoder: JwtDecoder) {
        cachedDecoder = decoder
    }

    fun verifyIdToken(idToken: String): FirebaseTokenClaims {
        return try {
            val jwt = activeDecoder.decode(idToken)
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
