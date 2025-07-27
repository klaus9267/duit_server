package duit.server.application.dto.user

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "로그인 ID는 필수입니다")
    val loginId: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String
)
