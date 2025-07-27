package duit.server.domain.user.entity

import duit.server.domain.bookmark.entity.Bookmark
import duit.server.domain.organizer.entity.Host
import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val email: String? = null,
    val loginId: String? = null,
    val password: String? = null,
    var nickname: String,
    val providerType: ProviderType? = null,
    val providerId: String? = null,
    val allowPushAlarm: Boolean = true,
    val allowMarketingAlarm: Boolean = true,

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookmarks: MutableList<Bookmark> = mutableListOf()

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val host: Host? = null
    
    /**
     * 닉네임 업데이트
     */
    fun updateNickname(newNickname: String) {
        this.nickname = newNickname
        this.updatedAt = LocalDateTime.now()
    }
}