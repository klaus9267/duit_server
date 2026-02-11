package duit.server.domain.auth.controller

import duit.server.application.security.JwtTokenProvider
import duit.server.domain.user.service.UserService
import duit.server.infrastructure.external.firebase.FirebaseUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Profile("!prod")
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth (Dev)", description = "테스트용 인증 API — 프로덕션 환경에서는 비활성화됩니다")
class DevAuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userService: UserService,
    private val firebaseUtil: FirebaseUtil
) {

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
