package duit.server.domain.user.repository

import duit.server.domain.user.entity.UserDeviceToken
import org.springframework.data.jpa.repository.JpaRepository

interface UserDeviceTokenRepository : JpaRepository<UserDeviceToken, Long> {
    fun findByToken(token: String): UserDeviceToken?
}