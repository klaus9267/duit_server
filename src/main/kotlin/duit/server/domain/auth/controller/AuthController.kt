package duit.server.domain.auth.controller

import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.auth.dto.SocialLoginRequest
import duit.server.domain.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 관련 API")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/social")
    @Operation(
        summary = "소셜 로그인",
        description = "Firebase ID 토큰으로 소셜 로그인 또는 회원가입을 수행합니다. " +
            "본문은 SocialLoginRequest JSON 이며, 구버전 클라이언트 호환을 위해 ID 토큰 원문만 보내도 됩니다. " +
            "agreedTermsVersion 을 보내면 동의한 약관 버전과 동의 시각을 기록합니다."
    )
    @ResponseStatus(HttpStatus.OK)
    fun socialLogin(
        @RequestBody
        @Schema(implementation = SocialLoginRequest::class)
        body: String,
    ): AuthResponse = SocialLoginRequest.parse(body)
        .let { authService.socialLogin(it.idToken, it.agreedTermsVersion) }
}
