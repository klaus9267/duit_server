package duit.server.domain.admin.dto

import duit.server.domain.admin.entity.Admin
import duit.server.domain.user.dto.UserResponse
import java.time.LocalDateTime

data class AdminResponse(
    val id: Long,
    val adminId: String,
    val userId: Long,
    val user: UserResponse,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(admin: Admin): AdminResponse {
            return AdminResponse(
                id = admin.id!!,
                adminId = admin.adminId,
                userId = admin.user.id!!,
                user = UserResponse.from(admin.user),
                createdAt = admin.createdAt
            )
        }
    }
}
