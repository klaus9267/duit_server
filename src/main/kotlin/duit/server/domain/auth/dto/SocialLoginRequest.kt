package duit.server.domain.auth.dto

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "소셜 로그인 요청")
data class SocialLoginRequest(
    @Schema(description = "Firebase ID 토큰", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
    val idToken: String,

    @Schema(description = "동의한 이용약관 버전 (yyyy-MM-dd, 선택)", example = "2026-10-15", maxLength = 20)
    val agreedTermsVersion: String? = null,
) {
    init {
        agreedTermsVersion?.let {
            require(isoDatePattern.matches(it) && runCatching { LocalDate.parse(it) }.isSuccess) {
                "약관 버전은 yyyy-MM-dd 형식이어야 합니다"
            }
        }
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        private val isoDatePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        /**
         * 구버전 앱/웹은 ID 토큰 원문을 그대로 본문에 보내고, 신버전은 JSON 객체를 보낸다.
         * 두 형식을 모두 받아야 하므로 본문을 문자열로 받아 여기서 해석한다.
         */
        fun parse(body: String): SocialLoginRequest {
            val trimmed = body.trim()
            if (!trimmed.startsWith("{")) return SocialLoginRequest(idToken = trimmed)

            return runCatching { objectMapper.readValue<SocialLoginRequest>(trimmed) }
                .getOrElse { e ->
                    throw generateSequence(e) { it.cause }.filterIsInstance<IllegalArgumentException>().firstOrNull()
                        ?: IllegalArgumentException("소셜 로그인 요청 형식이 올바르지 않습니다")
                }
        }
    }
}
