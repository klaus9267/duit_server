package duit.server.domain.admin.repository

import duit.server.domain.admin.dto.AdminResponse
import duit.server.domain.admin.entity.Admin
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AdminRepository : JpaRepository<Admin, Long> {
    fun findAllByAdminId(adminId: String): List<Admin>

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Admin a WHERE a.user.id = :userId")
    fun existsByUserId(@Param("userId") userId: Long): Boolean

    @Query(
        value = """
            SELECT new duit.server.domain.admin.dto.AdminResponse(
                a.id, a.adminId, u.id, u.nickname, u.email, a.createdAt
            )
            FROM Admin a
            JOIN a.user u
        """,
        countQuery = "SELECT COUNT(a) FROM Admin a"
    )
    fun findAllAdmins(pageable: Pageable): Page<AdminResponse>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Admin a ORDER BY a.id")
    fun findAllForUpdate(): List<Admin>

    @Modifying
    @Query("DELETE FROM Admin a WHERE a.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long): Int
}
