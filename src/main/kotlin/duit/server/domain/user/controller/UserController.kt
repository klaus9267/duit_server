package duit.server.domain.user.controller

import duit.server.domain.common.docs.AuthApiResponses
import duit.server.domain.common.docs.CommonApiResponses
import duit.server.domain.user.controller.docs.CheckNicknameDuplicateApi
import duit.server.domain.user.controller.docs.GetCurrentUserApi
import duit.server.domain.user.controller.docs.UpdateCurrentUserNicknameApi
import duit.server.domain.user.controller.docs.UpdateDevice
import duit.server.domain.user.controller.docs.WithdrawApi
import duit.server.domain.user.dto.UpdateNicknameRequest
import duit.server.domain.user.dto.UpdateUserSettingsRequest
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.service.UserService
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
    @CheckNicknameDuplicateApi
    @CommonApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun checkNicknameDuplicate(
        @Parameter(description = "확인할 닉네임", required = true)
        @RequestParam nickname: String
    ): Unit = userService.checkNicknameAvailable(nickname)

    @GetMapping("/me")
    @GetCurrentUserApi
    @AuthApiResponses
    @CommonApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun getCurrentUser(): UserResponse = userService.getCurrentUser()

    @PatchMapping("/nickname")
    @UpdateCurrentUserNicknameApi
    @AuthApiResponses
    @CommonApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun updateCurrentUserNickname(
        @Valid @RequestBody request: UpdateNicknameRequest
    ): UserResponse = userService.updateCurrentUserNickname(request)

    @PatchMapping("/device/{token}")
    @UpdateDevice
    @AuthApiResponses
    @CommonApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun updateDevice(@PathVariable token: String) = userService.updateDevice(token)

    @PatchMapping("/settings")
    @io.swagger.v3.oas.annotations.Operation(
        summary = "사용자 설정 수정", 
        description = "현재 사용자의 알림 설정 및 캘린더 자동 추가 설정을 수정합니다."
    )
    @AuthApiResponses
    @CommonApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun updateUserSettings(
        @Valid @RequestBody request: UpdateUserSettingsRequest
    ): UserResponse = userService.updateUserSettings(request)

    @DeleteMapping("/{userId}")
    @WithdrawApi
    @AuthApiResponses
    @CommonApiResponses
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable userId: Long
    ) = userService.withdraw()
}