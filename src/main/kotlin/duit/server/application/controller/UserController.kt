package duit.server.application.controller

import duit.server.application.controller.dto.user.UpdateNicknameRequest
import duit.server.application.controller.dto.user.UserResponse
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
    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임의 중복 여부를 확인합니다")
    @ResponseStatus(HttpStatus.OK)
    fun checkNicknameDuplicate(
        @Parameter(description = "확인할 닉네임", required = true)
        @RequestParam nickname: String
    ): Unit = userService.checkNicknameAvailable(nickname)

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 조회", description = "사용자 정보를 조회합니다")
    @ResponseStatus(HttpStatus.OK)
    fun getUser(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable userId: Long
    ): UserResponse = userService.getUser(userId)

    @PatchMapping("/{userId}/nickname")
    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 수정합니다")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateNickname(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable userId: Long,
        @Valid @RequestBody request: UpdateNicknameRequest
    ): UserResponse = userService.updateNickname(userId, request)

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "회원탈퇴", description = "사용자를 탈퇴시킵니다")
    fun withdraw() = userService.withdraw()
}
