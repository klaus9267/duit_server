package duit.server.domain.user.entity

import duit.server.domain.bookmark.entity.Bookmark
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
    var nickname: String,
    val providerType: ProviderType? = null,
    val providerId: String? = null,
    var autoAddBookmarkToCalendar: Boolean = false,
    @Embedded
    var alarmSettings: AlarmSettings = AlarmSettings(),
    var deviceToken: String? = null,

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookmarks: MutableList<Bookmark> = mutableListOf()

    /**
     * 닉네임 업데이트
     */
    fun updateNickname(newNickname: String) {
        this.nickname = newNickname
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 사용자 설정 통합 업데이트 (알림 설정 + 캘린더 설정)
     */
    fun updateSettings(newAlarmSettings: AlarmSettings, autoAddBookmarkToCalendar: Boolean) {
        this.alarmSettings = newAlarmSettings
        this.autoAddBookmarkToCalendar = autoAddBookmarkToCalendar
        this.updatedAt = LocalDateTime.now()
    }
}