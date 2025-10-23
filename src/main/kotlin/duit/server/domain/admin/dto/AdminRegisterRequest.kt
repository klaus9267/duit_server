package duit.server.domain.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class AdminRegisterRequest(
    @field:Positive(message = "유효한 사용자 ID를 입력하세요")
    val userId: Long,

    @field:NotBlank(message = "관리자 ID는 필수입니다")
    @field:Size(min = 4, max = 50, message = "관리자 ID는 4~50자 사이여야 합니다")
    val adminId: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    val password: String
)
