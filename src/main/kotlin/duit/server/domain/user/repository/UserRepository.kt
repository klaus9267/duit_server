package duit.server.domain.user.repository

import duit.server.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun existsByNickname(nickname: String): Boolean
    fun findByProviderId(providerId: String): User?
}
