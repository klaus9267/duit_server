package duit.server.domain.auth.controller

import duit.server.domain.auth.dto.AuthResponse
import duit.server.domain.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
    @Operation(summary = "소셜 로그인", description = "Firebase ID 토큰으로 소셜 로그인 또는 회원가입을 수행합니다")
    @ResponseStatus(HttpStatus.OK)
    fun socialLogin(
        @Valid @RequestBody
        @Schema(description = "Firebase ID 토큰", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
        idToken: String,
    ): AuthResponse = authService.socialLogin(idToken)
}
