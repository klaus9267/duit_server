package duit.server.domain.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class AdminRegisterRequest(
    @field:Schema(description = "관리자로 등록할 사용자 ID", example = "10")
    @field:Positive(message = "사용자 ID는 양수여야 합니다")
    val userId: Long,

    @field:Schema(description = "관리자 로그인 ID (4~50자)", example = "dutyit-admin")
    @field:NotBlank(message = "관리자 ID는 필수입니다")
    @field:Size(min = 4, max = 50, message = "관리자 ID는 4~50자 사이여야 합니다")
    val adminId: String,

    @field:Schema(description = "비밀번호 (8자 이상)", example = "password1234")
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    val password: String
)
