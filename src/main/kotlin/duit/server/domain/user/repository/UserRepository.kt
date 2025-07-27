package duit.server.domain.user.repository

import duit.server.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    fun findByLoginId(loginId: String): Optional<User>
    
    fun existsByLoginId(loginId: String): Boolean
    
    fun existsByEmail(email: String): Boolean
    
    fun existsByNickname(nickname: String): Boolean
}
