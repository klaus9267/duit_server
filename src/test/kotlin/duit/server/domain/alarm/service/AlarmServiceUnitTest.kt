package duit.server.domain.alarm.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.repository.AlarmRepository
import duit.server.domain.bookmark.repository.BookmarkRepository
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.entity.Host
import duit.server.domain.user.entity.User
import duit.server.infrastructure.external.firebase.FCMService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("AlarmService 단위 테스트")
class AlarmServiceUnitTest {

    private lateinit var fcmService: FCMService
    private lateinit var eventRepository: EventRepository
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var securityUtil: SecurityUtil
    private lateinit var alarmService: AlarmService

    private lateinit var host: Host
    private lateinit var user: User
    private lateinit var event: Event

    @BeforeEach
    fun setUp() {
        fcmService = mockk(relaxed = true)
        eventRepository = mockk()
        alarmRepository = mockk()
        bookmarkRepository = mockk()
        securityUtil = mockk()
        alarmService = AlarmService(fcmService, eventRepository, alarmRepository, bookmarkRepository, securityUtil)

        host = Host(id = 1L, name = "테스트 주최")
        user = User(id = 1L, nickname = "테스트유저", providerId = "p1", deviceToken = "fcm-token-1").apply {
            registerDeviceToken("fcm-token-1")
        }
        event = Event(
            id = 10L, title = "테스트 행사",
            startAt = LocalDateTime.now().plusDays(1), endAt = null,
            recruitmentStartAt = LocalDateTime.now().minusDays(1),
            recruitmentEndAt = LocalDateTime.now().plusHours(12),
            uri = "https://example.com", thumbnail = null,
            eventType = EventType.CONFERENCE, host = host
        )
    }

    @Nested
    @DisplayName("markAsRead")
    inner class MarkAsReadTests {

        @Test
        @DisplayName("알람을 찾으면 isRead=true로 설정하고 저장한다")
        fun marksAlarmAsRead() {
            val alarm = Alarm(id = 1L, user = user, event = event, type = AlarmType.EVENT_START, isRead = false)
            every { securityUtil.getCurrentUserId() } returns 1L
            every { alarmRepository.findByUserIdAndId(1L, 1L) } returns alarm
            every { alarmRepository.save(alarm) } returns alarm

            alarmService.markAsRead(1L)

            assertTrue(alarm.isRead)
            verify(exactly = 1) { alarmRepository.save(alarm) }
        }

        @Test
        @DisplayName("알람을 찾지 못하면 IllegalArgumentException이 발생한다")
        fun throwsWhenNotFound() {
            every { securityUtil.getCurrentUserId() } returns 1L
            every { alarmRepository.findByUserIdAndId(1L, 999L) } returns null

            assertThrows<IllegalArgumentException> {
                alarmService.markAsRead(999L)
            }
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    inner class MarkAllAsReadTests {

        @Test
        @DisplayName("모든 알람을 isRead=true로 설정한다")
        fun marksAllAsRead() {
            val alarm1 = Alarm(id = 1L, user = user, event = event, type = AlarmType.EVENT_START, isRead = false)
            val alarm2 = Alarm(id = 2L, user = user, event = event, type = AlarmType.RECRUITMENT_END, isRead = false)
            val page = PageImpl(listOf(alarm1, alarm2))

            every { securityUtil.getCurrentUserId() } returns 1L
            every { alarmRepository.findByUserId(1L, any()) } returns page
            every { alarmRepository.saveAll(any<List<Alarm>>()) } answers { firstArg() }

            alarmService.markAllAsRead()

            assertTrue(alarm1.isRead)
            assertTrue(alarm2.isRead)
        }
    }

    @Nested
    @DisplayName("deleteAlarm")
    inner class DeleteAlarmTests {

        @Test
        @DisplayName("알람을 찾으면 삭제한다")
        fun deletesAlarm() {
            val alarm = Alarm(id = 1L, user = user, event = event, type = AlarmType.EVENT_START)
            every { securityUtil.getCurrentUserId() } returns 1L
            every { alarmRepository.findByUserIdAndId(1L, 1L) } returns alarm
            every { alarmRepository.delete(alarm) } just runs

            alarmService.deleteAlarm(1L)

            verify(exactly = 1) { alarmRepository.delete(alarm) }
        }

        @Test
        @DisplayName("알람을 찾지 못하면 IllegalArgumentException이 발생한다")
        fun throwsWhenNotFound() {
            every { securityUtil.getCurrentUserId() } returns 1L
            every { alarmRepository.findByUserIdAndId(1L, 999L) } returns null

            assertThrows<IllegalArgumentException> {
                alarmService.deleteAlarm(999L)
            }
        }
    }

    @Nested
    @DisplayName("deleteAlarms")
    inner class DeleteAlarmsTests {

        @Test
        @DisplayName("readOnly=true이면 읽은 알람만 삭제한다")
        fun deletesReadOnlyAlarms() {
            every { securityUtil.getCurrentUserId() } returns 1L
            every { alarmRepository.deleteByUserIdAndIsRead(1L, true) } just runs

            alarmService.deleteAlarms(readOnly = true)

            verify(exactly = 1) { alarmRepository.deleteByUserIdAndIsRead(1L, true) }
            verify(exactly = 0) { alarmRepository.deleteByUserId(any()) }
        }

        @Test
        @DisplayName("readOnly=false이면 전체 알람을 삭제한다")
        fun deletesAllAlarms() {
            every { securityUtil.getCurrentUserId() } returns 1L
            every { alarmRepository.deleteByUserId(1L) } just runs

            alarmService.deleteAlarms(readOnly = false)

            verify(exactly = 1) { alarmRepository.deleteByUserId(1L) }
            verify(exactly = 0) { alarmRepository.deleteByUserIdAndIsRead(any(), any()) }
        }
    }

    @Nested
    @DisplayName("createAlarms - 스케줄러 알람 생성")
    inner class CreateAlarmsTests {

        @Test
        @DisplayName("이벤트가 존재하지 않으면 아무것도 하지 않는다")
        fun earlyReturnWhenEventNotFound() {
            every { eventRepository.findById(999L) } returns Optional.empty()

            alarmService.createAlarms(AlarmType.EVENT_START, 999L)

            verify(exactly = 0) { bookmarkRepository.findEligibleUsersForAlarms(any()) }
            verify(exactly = 0) { fcmService.sendAlarms(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("적격 사용자가 없으면 알람을 생성하지 않는다")
        fun earlyReturnWhenNoEligibleUsers() {
            every { eventRepository.findById(10L) } returns Optional.of(event)
            every { bookmarkRepository.findEligibleUsersForAlarms(10L) } returns emptyList()

            alarmService.createAlarms(AlarmType.EVENT_START, 10L)

            verify(exactly = 0) { alarmRepository.save(any()) }
            verify(exactly = 0) { fcmService.sendAlarms(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("적격 사용자에게 알람을 생성하고 FCM을 전송한다")
        fun createsAlarmsAndSendsFcm() {
            every { eventRepository.findById(10L) } returns Optional.of(event)
            every { bookmarkRepository.findEligibleUsersForAlarms(10L) } returns listOf(user)
            every { alarmRepository.existsByUserIdAndEventIdAndType(1L, 10L, AlarmType.EVENT_START) } returns false
            every { alarmRepository.save(any<Alarm>()) } answers { firstArg() }
            user.registerDeviceToken("fcm-token-1-sub")

            alarmService.createAlarms(AlarmType.EVENT_START, 10L)

            verify(exactly = 1) { alarmRepository.save(any()) }
            verify(exactly = 1) { fcmService.sendAlarms(listOf("fcm-token-1", "fcm-token-1-sub"), any(), any(), any()) }
        }

        @Test
        @DisplayName("이미 알람이 존재하는 사용자는 중복 생성하지 않고 FCM도 전송하지 않는다")
        fun skipsExistingAlarm() {
            every { eventRepository.findById(10L) } returns Optional.of(event)
            every { bookmarkRepository.findEligibleUsersForAlarms(10L) } returns listOf(user)
            every { alarmRepository.existsByUserIdAndEventIdAndType(1L, 10L, AlarmType.EVENT_START) } returns true

            alarmService.createAlarms(AlarmType.EVENT_START, 10L)

            verify(exactly = 0) { alarmRepository.save(any()) }
            verify(exactly = 0) { fcmService.sendAlarms(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("여러 사용자 중 일부만 알람이 존재하면 새 사용자에게만 알람 생성한다")
        fun `여러 사용자 중 기존 알람이 있는 사용자는 건너뛴다`() {
            val user2 = User(id = 2L, nickname = "유저2", providerId = "p2", deviceToken = "fcm-token-2").apply {
                registerDeviceToken("fcm-token-2")
                registerDeviceToken("fcm-token-2-sub")
            }
            val user3 = User(id = 3L, nickname = "유저3", providerId = "p3", deviceToken = "fcm-token-3").apply {
                registerDeviceToken("fcm-token-3")
            }

            every { eventRepository.findById(10L) } returns Optional.of(event)
            every { bookmarkRepository.findEligibleUsersForAlarms(10L) } returns listOf(user, user2, user3)

            every { alarmRepository.existsByUserIdAndEventIdAndType(1L, 10L, AlarmType.EVENT_START) } returns true
            every { alarmRepository.existsByUserIdAndEventIdAndType(2L, 10L, AlarmType.EVENT_START) } returns false
            every { alarmRepository.existsByUserIdAndEventIdAndType(3L, 10L, AlarmType.EVENT_START) } returns false
            every { alarmRepository.save(any<Alarm>()) } answers { firstArg() }

            alarmService.createAlarms(AlarmType.EVENT_START, 10L)

            verify(exactly = 2) { alarmRepository.save(any()) }
            verify(exactly = 1) {
                fcmService.sendAlarms(listOf("fcm-token-2", "fcm-token-2-sub", "fcm-token-3"), any(), any(), any())
            }
        }

        @Test
        @DisplayName("한 유저의 알람이 이미 존재해도(UK 위반) 나머지 유저 알람은 정상 생성되어야 한다")
        fun `한 유저 save 실패해도 나머지 유저 알람은 생성된다`() {
            val user2 = User(id = 2L, nickname = "유저2", providerId = "p2", deviceToken = "fcm-token-2").apply {
                registerDeviceToken("fcm-token-2")
            }
            every { eventRepository.findById(10L) } returns Optional.of(event)
            every { bookmarkRepository.findEligibleUsersForAlarms(10L) } returns listOf(user, user2)
            every { alarmRepository.existsByUserIdAndEventIdAndType(any(), 10L, AlarmType.EVENT_START) } returns false
            var saveCount = 0
            every { alarmRepository.save(any<Alarm>()) } answers {
                saveCount++
                if (saveCount == 1) throw DataIntegrityViolationException("Duplicate entry")
                firstArg()
            }

            alarmService.createAlarms(AlarmType.EVENT_START, 10L)

            verify(exactly = 2) { alarmRepository.save(any()) }
            verify(exactly = 1) { fcmService.sendAlarms(listOf("fcm-token-2"), any(), any(), any()) }
        }

        @Test
        @DisplayName("동시 삽입으로 UK 위반이 발생해도 예외 없이 정상 완료되어야 한다")
        fun `race condition으로 UK 위반 발생해도 예외 전파 없이 처리된다`() {
            every { eventRepository.findById(10L) } returns Optional.of(event)
            every { bookmarkRepository.findEligibleUsersForAlarms(10L) } returns listOf(user)
            every { alarmRepository.existsByUserIdAndEventIdAndType(1L, 10L, AlarmType.EVENT_START) } returns false
            every { alarmRepository.save(any<Alarm>()) } throws DataIntegrityViolationException("Duplicate entry for key 'uk_user_event_type'")
            alarmService.createAlarms(AlarmType.EVENT_START, 10L)

            verify(exactly = 0) { fcmService.sendAlarms(any(), any(), any(), any()) }
        }

    }

    @Nested
    @DisplayName("createAlarms - FCM 메시지 내용 검증")
    inner class AlarmContentTests {

        @BeforeEach
        fun setUpMocks() {
            every { eventRepository.findById(10L) } returns Optional.of(event)
            every { bookmarkRepository.findEligibleUsersForAlarms(10L) } returns listOf(user)
            every { alarmRepository.existsByUserIdAndEventIdAndType(any(), any(), any()) } returns false
            every { alarmRepository.save(any<Alarm>()) } answers { firstArg() }
        }

        @Test
        @DisplayName("EVENT_START: 제목에 '시작' 포함, 본문에 행사명과 시작 시각 포함, data에 event_start 타입")
        fun `EVENT_START FCM 내용 검증`() {
            alarmService.createAlarms(AlarmType.EVENT_START, 10L)

            val titleSlot = slot<String>()
            val bodySlot = slot<String>()
            val dataSlot = slot<Map<String, String>>()
            verify { fcmService.sendAlarms(any(), capture(titleSlot), capture(bodySlot), capture(dataSlot)) }

            assertTrue(titleSlot.captured.contains("시작"))
            assertTrue(bodySlot.captured.contains(event.title))
            assertTrue(bodySlot.captured.contains("${event.startAt.hour}시"))
            assertEquals("event_start", dataSlot.captured["type"])
            assertEquals(event.id.toString(), dataSlot.captured["eventId"])
            assertEquals(event.host.name, dataSlot.captured["hostName"])
        }

        @Test
        @DisplayName("RECRUITMENT_START: 제목에 '모집' 포함, 본문에 행사명과 모집 시작 시각 포함")
        fun `RECRUITMENT_START FCM 내용 검증`() {
            alarmService.createAlarms(AlarmType.RECRUITMENT_START, 10L)

            val titleSlot = slot<String>()
            val bodySlot = slot<String>()
            val dataSlot = slot<Map<String, String>>()
            verify { fcmService.sendAlarms(any(), capture(titleSlot), capture(bodySlot), capture(dataSlot)) }

            assertTrue(titleSlot.captured.contains("모집"))
            assertTrue(titleSlot.captured.contains("시작"))
            assertTrue(bodySlot.captured.contains(event.title))
            assertTrue(bodySlot.captured.contains("${event.recruitmentStartAt!!.hour}시"))
            assertEquals("recruitment_start", dataSlot.captured["type"])
            assertEquals(event.id.toString(), dataSlot.captured["eventId"])
        }

        @Test
        @DisplayName("RECRUITMENT_END: 제목에 '마감' 포함, 본문에 행사명과 모집 마감 시각 포함")
        fun `RECRUITMENT_END FCM 내용 검증`() {
            alarmService.createAlarms(AlarmType.RECRUITMENT_END, 10L)

            val titleSlot = slot<String>()
            val bodySlot = slot<String>()
            val dataSlot = slot<Map<String, String>>()
            verify { fcmService.sendAlarms(any(), capture(titleSlot), capture(bodySlot), capture(dataSlot)) }

            assertTrue(titleSlot.captured.contains("마감"))
            assertTrue(bodySlot.captured.contains(event.title))
            assertTrue(bodySlot.captured.contains("${event.recruitmentEndAt!!.hour}시"))
            assertEquals("recruitment_end", dataSlot.captured["type"])
            assertEquals(event.id.toString(), dataSlot.captured["eventId"])
        }
    }
}
