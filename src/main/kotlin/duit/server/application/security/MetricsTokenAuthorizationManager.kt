package duit.server.application.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.function.Supplier

/**
 * /actuator/prometheus 스크랩 전용 Bearer 토큰 검증.
 * 토큰이 비어 있으면 모든 요청을 거부하고, 길이 차이도 드러나지 않도록 해시끼리 상수 시간 비교한다.
 */
@Component
class MetricsTokenAuthorizationManager(
    @Value("\${metrics.token:}") metricsToken: String,
) : AuthorizationManager<RequestAuthorizationContext> {

    private val expectedDigest: ByteArray? = metricsToken.trim().takeIf { it.isNotEmpty() }?.let(::sha256)

    @Deprecated("deprecated 됐지만 AuthorizationManager 가 여전히 구현을 요구하는 추상 메서드")
    override fun check(
        authentication: Supplier<Authentication>,
        context: RequestAuthorizationContext,
    ): AuthorizationDecision = AuthorizationDecision(isValid(context.request.getHeader(HttpHeaders.AUTHORIZATION)))

    fun isValid(authorizationHeader: String?): Boolean {
        val expected = expectedDigest ?: return false
        val provided = authorizationHeader
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)
            ?.trim()
            ?: return false
        return MessageDigest.isEqual(expected, sha256(provided))
    }

    private fun sha256(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
