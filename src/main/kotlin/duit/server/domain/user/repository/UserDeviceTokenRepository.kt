package duit.server.domain.user.repository

import duit.server.domain.user.entity.UserDeviceToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserDeviceTokenRepository : JpaRepository<UserDeviceToken, Long> {
    fun findByToken(token: String): UserDeviceToken?

    @Query("SELECT t.token FROM UserDeviceToken t WHERE t.user.id = :userId")
    fun findTokensByUserId(@Param("userId") userId: Long): List<String>

    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.token IN :tokens")
    fun deleteAllByTokenIn(@Param("tokens") tokens: Collection<String>): Int
}
