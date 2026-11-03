package duit.server.domain.admin.dto

import duit.server.domain.admin.entity.Admin
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminResponse(
    @field:Schema(description = "관리자 ID (해제 시 사용)", example = "1")
    val id: Long,

    @field:Schema(description = "관리자 로그인 ID", example = "dutyit-admin")
    val adminId: String,

    @field:Schema(description = "연결된 사용자 ID", example = "10")
    val userId: Long,

    @field:Schema(description = "사용자 닉네임", example = "듀잇")
    val nickname: String,

    @field:Schema(description = "사용자 이메일", example = "admin@dutyit.net", nullable = true)
    val email: String?,

    @field:Schema(description = "관리자 등록 일시")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(admin: Admin) = AdminResponse(
            id = admin.id!!,
            adminId = admin.adminId,
            userId = admin.user.id!!,
            nickname = admin.user.nickname,
            email = admin.user.email,
            createdAt = admin.createdAt
        )
    }
}
