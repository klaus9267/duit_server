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
import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.JobBookmark
import duit.server.domain.job.entity.JobCompany
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.JobPostingWork24Detail
import duit.server.domain.job.entity.SalaryType
import duit.server.domain.job.entity.WorkRegion
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
        deviceTokens: List<String> = emptyList(),
    ): User {
        val user = User(
            email = email,
            nickname = nickname,
            providerType = providerType,
            providerId = providerId,
            alarmSettings = alarmSettings,
            deviceToken = deviceToken ?: deviceTokens.firstOrNull(),
        )
        (listOfNotNull(deviceToken) + deviceTokens).distinct().forEach(user::registerDeviceToken)
        return user
    }

    fun event(
        title: String = "테스트 행사",
        startAt: LocalDateTime = LocalDateTime.now().plusDays(7),
        endAt: LocalDateTime? = LocalDateTime.now().plusDays(8),
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
        uri: String = "https://example.com/event",
        thumbnail: String? = null,
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

    fun jobPosting(
        wantedAuthNo: String = "test-${System.nanoTime()}",
        title: String = "테스트 간호사 채용",
        companyName: String = "테스트 병원",
        jobCategory: String? = "간호사",
        location: String? = "서울특별시 강남구",
        workRegion: WorkRegion? = WorkRegion.SEOUL,
        workDistrict: String? = "강남구",
        employmentType: EmploymentType? = EmploymentType.FULL_TIME,
        careerMin: Int? = null,
        careerMax: Int? = null,
        educationLevel: EducationLevel? = EducationLevel.ASSOCIATE,
        salaryMin: Long? = 3500,
        salaryMax: Long? = 4500,
        salaryType: SalaryType? = SalaryType.ANNUAL,
        postingUrl: String = "https://example.com/job",
        postedAt: LocalDateTime? = LocalDateTime.now().minusDays(1),
        expiresAt: LocalDateTime? = LocalDateTime.now().plusDays(30),
        closeType: CloseType = CloseType.FIXED,
        isActive: Boolean = true,
        workHoursPerWeek: Int? = 40,
    ): JobPosting = JobPosting(
        wantedAuthNo = wantedAuthNo,
        isActive = isActive,
    ).apply {
        updateWork24Detail(
            detail = JobPostingWork24Detail(
                jobsNm = jobCategory,
                wantedTitle = title,
                receiptCloseDt = when (closeType) {
                    CloseType.ON_HIRE -> "채용시까지"
                    CloseType.ONGOING -> "상시"
                    CloseType.FIXED -> expiresAt?.toString()
                },
                empTpNm = employmentType?.displayName,
                salTpNm = salaryType?.displayName,
                enterTpNm = when {
                    careerMin == null && careerMax == null -> null
                    careerMin == 0 && careerMax == null -> "신입"
                    careerMin != null && careerMax == null -> "경력 ${careerMin}년 이상"
                    careerMin != null && careerMax != null -> "경력 ${careerMin}~${careerMax}년"
                    else -> "경력무관"
                },
                eduNm = educationLevel?.displayName,
                workRegion = workRegion?.displayName ?: location,
                dtlRecrContUrl = postingUrl,
                jobsCd = jobCategory,
                empTpCd = employmentType?.name,
                salTpCd = salaryType?.name,
                workdayWorkhrCont = workHoursPerWeek?.let { "주 ${it}시간" },
            ),
            company = JobCompany(corpNm = companyName)
        )
    }

    fun jobBookmark(
        user: User,
        jobPosting: JobPosting,
    ): JobBookmark = JobBookmark(
        user = user,
        jobPosting = jobPosting,
    )
}
