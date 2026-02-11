package duit.server.support.fixture

import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.entity.BannedIp
import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.bookmark.entity.Bookmark
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.AlarmSettings
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.view.entity.View
import java.time.LocalDateTime

object TestFixtures {

    fun host(
        name: String = "테스트 주최자",
        thumbnail: String? = null,
    ): Host = Host(
        name = name,
        thumbnail = thumbnail,
    )

    fun user(
        email: String? = "test@example.com",
        nickname: String = "테스트유저",
        providerType: ProviderType? = ProviderType.GOOGLE,
        providerId: String? = "test-provider-id",
        alarmSettings: AlarmSettings = AlarmSettings(),
        deviceToken: String? = null,
    ): User = User(
        email = email,
        nickname = nickname,
        providerType = providerType,
        providerId = providerId,
        alarmSettings = alarmSettings,
        deviceToken = deviceToken,
    )

    fun event(
        title: String = "테스트 행사",
        startAt: LocalDateTime = LocalDateTime.now().plusDays(7),
        endAt: LocalDateTime? = LocalDateTime.now().plusDays(8),
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
        uri: String = "https://example.com/event",
        thumbnail: String? = null,
        isApproved: Boolean = false,
        status: EventStatus = EventStatus.PENDING,
        statusGroup: EventStatusGroup = EventStatusGroup.PENDING,
        eventType: EventType = EventType.CONFERENCE,
        host: Host,
    ): Event = Event(
        title = title,
        startAt = startAt,
        endAt = endAt,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        uri = uri,
        thumbnail = thumbnail,
        isApproved = isApproved,
        status = status,
        statusGroup = statusGroup,
        eventType = eventType,
        host = host,
    )

    fun bookmark(
        user: User,
        event: Event,
        isAddedToCalendar: Boolean = false,
    ): Bookmark = Bookmark(
        user = user,
        event = event,
        isAddedToCalendar = isAddedToCalendar,
    )

    fun view(
        event: Event,
        count: Int = 0,
    ): View = View(
        event = event,
        count = count,
    )

    fun alarm(
        user: User,
        event: Event,
        type: AlarmType = AlarmType.EVENT_START,
        isRead: Boolean = false,
    ): Alarm = Alarm(
        user = user,
        event = event,
        type = type,
        isRead = isRead,
    )

    fun admin(
        user: User,
        adminId: String = "testadmin",
        password: String = "encoded-password",
    ): Admin = Admin(
        user = user,
        adminId = adminId,
        password = password,
    )

    fun bannedIp(
        ipAddress: String = "192.168.1.100",
        failureCount: Int = 0,
        isBanned: Boolean = false,
    ): BannedIp = BannedIp(
        ipAddress = ipAddress,
        failureCount = failureCount,
        isBanned = isBanned,
    )
}
