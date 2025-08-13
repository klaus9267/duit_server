package duit.server.domain.auth.dto

import duit.server.domain.user.dto.UserResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "인증 응답")
data class AuthResponse(
    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
    val accessToken: String,
    
    @Schema(description = "사용자 정보")
    val user: UserResponse,
    
    @Schema(description = "신규 가입 여부", example = "true")
    val isNewUser: Boolean
)