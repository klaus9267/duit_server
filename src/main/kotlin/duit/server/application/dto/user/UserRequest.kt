package duit.server.application.dto.user

import duit.server.domain.user.entity.User
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UserRequest(
    @field:NotBlank(message = "로그인 ID는 필수입니다")
    @field:Size(min = 4, max = 20, message = "로그인 ID는 4-20자 사이여야 합니다")
    @field:Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "로그인 ID는 영문, 숫자, 언더스코어만 사용 가능합니다")
    val loginId: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+$",
        message = "비밀번호는 영문, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다"
    )
    val password: String,
    
    @field:NotBlank(message = "닉네임은 필수입니다")
    @field:Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다")
    val nickname: String,
) {
    fun toEntity(hashedPassword: String) = User(
        loginId = loginId,
        password = hashedPassword,
        nickname = nickname
    )
}
