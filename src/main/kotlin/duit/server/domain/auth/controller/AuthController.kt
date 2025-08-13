package duit.server.domain.auth.controller

import duit.server.application.docs.auth.IssueTokenApi
import duit.server.application.docs.auth.SocialLoginApi
import duit.server.application.docs.common.CommonApiResponses
import duit.server.application.security.JwtTokenProvider
import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.auth.service.AuthService
import duit.server.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 관련 API")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userService: UserService,
    private val authService: AuthService
) {

    @PostMapping("/social")
    @SocialLoginApi
    @CommonApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun socialLogin(
        @Valid @RequestBody
        @Schema(description = "Firebase ID 토큰", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
        idToken: String,
    ): AuthResponse = authService.socialLogin(idToken)

    /**
     * 개발/테스트용 간단한 토큰 발급 API
     * 실제 운영에서는 소셜 로그인이나 이메일/비밀번호 인증 후 토큰 발급
     */
    @PostMapping("/token")
    @IssueTokenApi
    @CommonApiResponses
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
