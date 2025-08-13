package duit.server.domain.user.controller

import duit.server.application.docs.common.AuthApiResponses
import duit.server.application.docs.common.CommonApiResponses
import duit.server.application.docs.user.*
import duit.server.domain.user.dto.UpdateNicknameRequest
import duit.server.domain.user.dto.UserResponse
import duit.server.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

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