package duit.server.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "소셜 로그인 요청")
data class SocialLoginRequest(
    @field:NotBlank(message = "ID 토큰은 필수입니다")
    @Schema(description = "Firebase ID 토큰", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
    val idToken: String
)
