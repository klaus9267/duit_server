package duit.server.domain.admin.controller

import duit.server.domain.admin.dto.AdminLoginRequest
import duit.server.domain.admin.dto.AdminLoginResponse
import duit.server.domain.admin.dto.AdminRegisterRequest
import duit.server.domain.admin.dto.AdminResponse
import duit.server.domain.admin.service.AdminAuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "Admin Auth", description = "관리자 인증 API")
class AdminAuthController(
    private val adminAuthService: AdminAuthService
) {

    @PostMapping("/login")
    @Operation(summary = "관리자 로그인", description = "관리자 ID와 비밀번호로 로그인")
    @ResponseStatus(HttpStatus.OK)
    fun login(
        @RequestBody @Valid request: AdminLoginRequest,
        httpRequest: HttpServletRequest
    ): AdminLoginResponse {
        val ip = getClientIp(httpRequest)
        return adminAuthService.login(request.adminId, request.password, ip)
    }

    @PostMapping("/register")
    @Operation(summary = "관리자 등록", description = "최초 관리자 등록")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody @Valid request: AdminRegisterRequest
    ): AdminResponse = adminAuthService.register(request)

    private fun getClientIp(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")?.split(",")?.first()?.trim()
            ?: request.getHeader("X-Real-IP")
            ?: request.remoteAddr
}
