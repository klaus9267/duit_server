package duit.server.domain.auth.controller

import duit.server.application.security.JwtTokenProvider
import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.auth.service.AuthService
import duit.server.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
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
    private val authService: AuthService,
    private val firebaseUtil: duit.server.infrastructure.external.firebase.FirebaseUtil
) {

    @PostMapping("/social")
    @Operation(summary = "소셜 로그인", description = "Firebase ID 토큰으로 소셜 로그인 또는 회원가입을 수행합니다")
    @ResponseStatus(HttpStatus.OK)
    fun socialLogin(
        @Valid @RequestBody
        @Schema(description = "Firebase ID 토큰", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
        idToken: String,
    ): AuthResponse = authService.socialLogin(idToken)

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

    @PostMapping("/id-token")
    @Operation(
        summary = "Firebase ID Token 발급 (테스트용)",
        description = "개발 및 테스트 용도로 이메일 기반 Firebase ID Token을 발급합니다"
    )
    @ResponseStatus(HttpStatus.OK)
    fun issueIdToken(
        @Parameter(description = "사용자 UID")
        @RequestParam uid: String
    ): String = firebaseUtil.createIdTokenByUid(uid)
}
