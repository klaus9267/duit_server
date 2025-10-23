package duit.server.domain.admin.repository

import duit.server.domain.admin.entity.Admin
import org.springframework.data.jpa.repository.JpaRepository

interface AdminRepository : JpaRepository<Admin, Long> {
    fun findByAdminId(adminId: String): Admin?
    fun existsByAdminId(adminId: String): Boolean
}
