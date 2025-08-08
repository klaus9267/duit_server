package duit.server.domain.common.controller

import duit.server.application.security.JwtTokenProvider
import duit.server.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 관련 API")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userService: UserService
) {
    
    /**
     * 개발/테스트용 간단한 토큰 발급 API
     * 실제 운영에서는 소셜 로그인이나 이메일/비밀번호 인증 후 토큰 발급
     */
    @PostMapping("/token")
    @Operation(
        summary = "JWT 토큰 발급", 
        description = "개발/테스트용 JWT 토큰을 발급합니다. 실제 운영에서는 소셜 로그인 등으로 대체될 예정입니다."
    )
    @ResponseStatus(HttpStatus.OK)
    fun issueToken(
        @Parameter(description = "사용자 ID", required = true)
        @RequestParam userId: Long
    ): TokenResponse {
        // 사용자 존재 여부 확인
        userService.findUserById(userId)
        
        // JWT 토큰 생성
        val accessToken = jwtTokenProvider.createAccessToken(userId)
        
        return TokenResponse(
            accessToken = accessToken,
            tokenType = "Bearer"
        )
    }
}

/**
 * 토큰 발급 응답 DTO
 */
data class TokenResponse(
    val accessToken: String,
    val tokenType: String
)

/**
 * 토큰 검증 응답 DTO
 */
data class TokenValidationResponse(
    val valid: Boolean,
    val userId: Long?,
    val message: String
)
