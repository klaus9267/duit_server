package duit.server.application.controller

import duit.server.application.dto.ApiResponse
import duit.server.application.dto.user.LoginRequest
import duit.server.application.dto.user.UpdateNicknameRequest
import duit.server.application.dto.user.UserRequest
import duit.server.application.dto.user.UserResponse
import duit.server.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/users")
@Tag(name = "User", description = "사용자 관련 API")
class UserController(
    private val userService: UserService
) {
    
    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
    fun join(@Valid @RequestBody request: UserRequest): ApiResponse<UserResponse> {
        val userResponse = userService.join(request)
        return ApiResponse.success(userResponse, "회원가입이 완료되었습니다")
    }
    
    @GetMapping("/check-login-id")
    @Operation(summary = "로그인 ID 중복 확인", description = "로그인 ID의 중복 여부를 확인합니다")
    fun checkLoginIdDuplicate(
        @Parameter(description = "확인할 로그인 ID", required = true)
        @RequestParam loginId: String
    ): ApiResponse<Boolean> {
        val isAvailable = userService.checkLoginIdAvailable(loginId)
        return ApiResponse.success(isAvailable, if (isAvailable) "사용 가능한 로그인 ID입니다" else "이미 사용 중인 로그인 ID입니다")
    }
    
    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임의 중복 여부를 확인합니다")
    fun checkNicknameDuplicate(
        @Parameter(description = "확인할 닉네임", required = true)
        @RequestParam nickname: String
    ): ApiResponse<Boolean> {
        val isAvailable = userService.checkNicknameAvailable(nickname)
        return ApiResponse.success(isAvailable, if (isAvailable) "사용 가능한 닉네임입니다" else "이미 사용 중인 닉네임입니다")
    }
    
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인을 수행합니다")
    fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<UserResponse> {
        val userResponse = userService.login(request)
        return ApiResponse.success(userResponse, "로그인이 완료되었습니다")
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "사용자 조회", description = "사용자 정보를 조회합니다")
    fun getUser(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable userId: Long
    ): ApiResponse<UserResponse> {
        val userResponse = userService.getUser(userId)
        return ApiResponse.success(userResponse)
    }
    
    @PatchMapping("/{userId}/nickname")
    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 수정합니다")
    fun updateNickname(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable userId: Long,
        @Valid @RequestBody request: UpdateNicknameRequest
    ): ApiResponse<UserResponse> {
        val userResponse = userService.updateNickname(userId, request)
        return ApiResponse.success(userResponse, "닉네임이 수정되었습니다")
    }
    
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "회원탈퇴", description = "사용자를 탈퇴시킵니다")
    fun withdraw(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable userId: Long
    ): ApiResponse<Unit> {
        userService.withdraw(userId)
        return ApiResponse.success(message = "회원탈퇴가 완료되었습니다")
    }
}