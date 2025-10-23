package duit.server.domain.admin.dto

import jakarta.validation.constraints.NotBlank

data class AdminLoginRequest(
    @field:NotBlank(message = "관리자 ID는 필수입니다")
    val adminId: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String
)
