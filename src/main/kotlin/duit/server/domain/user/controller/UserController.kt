package duit.server.domain.user.controller

import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.user.controller.docs.CheckNicknameDuplicateApi
import duit.server.domain.user.controller.docs.GetCurrentUserApi
import duit.server.domain.user.controller.docs.UpdateCurrentUserNicknameApi
import duit.server.domain.user.controller.docs.UpdateDevice
import duit.server.domain.user.controller.docs.WithdrawApi
import duit.server.domain.user.dto.UpdateNicknameRequest
import duit.server.domain.user.dto.UpdateUserSettingsRequest
import duit.server.domain.user.dto.UserPaginationParam
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/users")
@Tag(name = "User", description = "사용자 관련 API")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    @Operation(summary = "사용자 목록 조회 (관리자)", description = "모든 사용자를 페이징하여 조회합니다")
    @ResponseStatus(HttpStatus.OK)
    fun getAllUsers(
        @Valid @ParameterObject param: UserPaginationParam
    ): PageResponse<UserResponse> = userService.getAllUsers(param)

    @GetMapping("/check-nickname")
    @CheckNicknameDuplicateApi
    @ResponseStatus(HttpStatus.OK)
    fun checkNicknameDuplicate(
        @Parameter(description = "확인할 닉네임", required = true)
        @RequestParam nickname: String
    ): Unit = userService.checkNicknameAvailable(nickname)

    @GetMapping("/me")
    @GetCurrentUserApi
    @ResponseStatus(HttpStatus.OK)
    fun getCurrentUser(): UserResponse = userService.getCurrentUser()

    @PatchMapping("/nickname")
    @UpdateCurrentUserNicknameApi
    @ResponseStatus(HttpStatus.OK)
    fun updateCurrentUserNickname(
        @Valid @RequestBody request: UpdateNicknameRequest
    ): UserResponse = userService.updateCurrentUserNickname(request)

    @PatchMapping("/device/{token}")
    @UpdateDevice
    @ResponseStatus(HttpStatus.OK)
    fun updateDevice(@PathVariable token: String) = userService.updateDevice(token)

    @PatchMapping("/settings")
    @io.swagger.v3.oas.annotations.Operation(
        summary = "사용자 설정 수정",
        description = "현재 사용자의 알림 설정 및 캘린더 자동 추가 설정을 수정합니다."
    )
    @ResponseStatus(HttpStatus.OK)
    fun updateUserSettings(
        @Valid @RequestBody request: UpdateUserSettingsRequest
    ): UserResponse = userService.updateUserSettings(request)

    @DeleteMapping("/{userId}")
    @WithdrawApi
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(
        @Parameter(description = "사용자 ID", required = true)
        @PathVariable userId: Long
    ) = userService.withdraw()
}