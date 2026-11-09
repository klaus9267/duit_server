package duit.server.domain.user.entity

import duit.server.domain.admin.entity.Admin
import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.bookmark.entity.Bookmark
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_provider", columnNames = ["provider_type", "provider_id"])
    ],
    indexes = [
        Index(name = "providerId", columnList = "provider_id"),
        Index(name = "nickname", columnList = "nickname"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
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

    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookmarks: MutableList<Bookmark> = mutableListOf()

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var admin: Admin? = null

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var alarms: MutableList<Alarm> = mutableListOf()

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var deviceTokens: MutableList<UserDeviceToken> = mutableListOf()

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

    fun registerDeviceToken(token: String) {
        if (deviceTokens.none { it.token == token }) {
            addDeviceToken(UserDeviceToken(user = this, token = token))
        }
    }

    // 한 기기에서 계정을 바꾸면 그 기기의 토큰은 마지막으로 로그인한 계정의 것이다
    fun takeOverDeviceToken(deviceToken: UserDeviceToken) {
        deviceToken.transferTo(this)
        addDeviceToken(deviceToken)
    }

    // 재설치·기기 변경으로 토큰이 계속 쌓이므로, 개수를 넘으면 가장 오래전에 등록된 토큰부터 지운다
    private fun addDeviceToken(deviceToken: UserDeviceToken) {
        deviceTokens.add(deviceToken)
        deviceTokens.sortedByDescending { it.updatedAt }
            .drop(MAX_DEVICE_TOKENS)
            .forEach(deviceTokens::remove)
    }

    fun unregisterDeviceToken(token: String) {
        deviceTokens.removeIf { it.token == token }
    }

    companion object {
        const val MAX_DEVICE_TOKENS = 10
    }
}
