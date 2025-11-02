package duit.server.domain.auth.controller

import duit.server.application.security.JwtTokenProvider
import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.auth.dto.SocialLoginRequest
import duit.server.domain.auth.service.AuthService
import duit.server.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

    @PostMapping("/social", consumes = ["application/json"])
    @Operation(
        summary = "소셜 로그인 (JSON)",
        description = "Firebase ID 토큰으로 소셜 로그인 또는 회원가입을 수행합니다 (JSON 형식)"
    )
    @ResponseStatus(HttpStatus.OK)
    fun socialLoginJson(
        @Valid @RequestBody request: SocialLoginRequest
    ): AuthResponse = authService.socialLogin(request.idToken)

    @PostMapping("/social", consumes = ["text/plain"])
    @Operation(
        summary = "소셜 로그인 (Plain Text)",
        description = "Firebase ID 토큰으로 소셜 로그인 또는 회원가입을 수행합니다 (Plain Text 형식)"
    )
    @ResponseStatus(HttpStatus.OK)
    fun socialLoginPlainText(
        @RequestBody idToken: String
    ): AuthResponse {
        require(idToken.isNotBlank()) { "ID 토큰이 비어있습니다" }
        return authService.socialLogin(idToken.trim())
    }

    @PostMapping("/token")
    @Operation(summary = "JWT 토큰 발급 (테스트용)", description = "개발 및 테스트 용도로 사용자 ID 기반 JWT 토큰을 발급합니다")
    @ResponseStatus(HttpStatus.OK)
    fun issueToken(
        @Parameter(description = "사용자 ID", required = true)
        @RequestParam userId: Long
    ): String {
        userService.findUserById(userId)
        return jwtTokenProvider.createAccessToken(userId)
    }
}
