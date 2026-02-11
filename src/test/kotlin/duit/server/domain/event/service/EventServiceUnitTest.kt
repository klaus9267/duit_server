package duit.server.domain.event.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.event.dto.*
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.dto.HostRequest
import duit.server.domain.host.entity.Host
import duit.server.domain.host.service.HostService
import duit.server.domain.view.entity.View
import duit.server.domain.view.service.ViewService
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.file.FileStorageService
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.*

@DisplayName("EventService 단위 테스트")
class EventServiceUnitTest {

    private fun createHost(id: Long = 1L, name: String = "테스트 주최", thumbnail: String? = null) =
        Host(id = id, name = name, thumbnail = thumbnail)

    private fun createEvent(
        id: Long = 1L,
        host: Host,
        title: String = "테스트 행사",
        startAt: LocalDateTime = LocalDateTime.now().plusDays(7),
        endAt: LocalDateTime? = LocalDateTime.now().plusDays(8),
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
        uri: String = "https://example.com",
        thumbnail: String? = null,
        eventType: EventType = EventType.CONFERENCE,
        status: EventStatus = EventStatus.RECRUITMENT_WAITING,
        statusGroup: EventStatusGroup = EventStatusGroup.ACTIVE,
    ) = Event(
        id = id, title = title, startAt = startAt, endAt = endAt,
        recruitmentStartAt = recruitmentStartAt, recruitmentEndAt = recruitmentEndAt,
        uri = uri, thumbnail = thumbnail, eventType = eventType, host = host,
        status = status, statusGroup = statusGroup,
    )

    private fun createEventService(
        eventRepository: EventRepository,
        viewService: ViewService,
        securityUtil: SecurityUtil,
        discordService: DiscordService,
        hostService: HostService,
        fileStorageService: FileStorageService,
    ) = EventService(eventRepository, viewService, securityUtil, discordService, hostService, fileStorageService)

    @Nested
    @DisplayName("createEvent")
    inner class CreateEventTests {
        private lateinit var eventRepository: EventRepository
        private lateinit var viewService: ViewService
        private lateinit var securityUtil: SecurityUtil
        private lateinit var discordService: DiscordService
        private lateinit var hostService: HostService
        private lateinit var fileStorageService: FileStorageService
        private lateinit var eventService: EventService

        @BeforeEach
        fun setUp() {
            eventRepository = mockk()
            viewService = mockk()
            securityUtil = mockk()
            discordService = mockk()
            hostService = mockk()
            fileStorageService = mockk()
            eventService = createEventService(eventRepository, viewService, securityUtil, discordService, hostService, fileStorageService)
        }

        @Test
        @DisplayName("hostId 사용 - 기존 Host로 Event 생성")
        fun createEventWithHostId() {
            val host = createHost()
            val savedEvent = createEvent(host = host)
            val eventRequest = EventCreateRequest(
                title = "테스트 행사", startAt = savedEvent.startAt, endAt = savedEvent.endAt,
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, hostName = null
            )

            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(any<Event>()) } returns savedEvent
            every { viewService.createView(any()) } returns mockk<View>(relaxed = true)

            val result = eventService.createEvent(eventRequest, null, null, true)

            verify(exactly = 1) { hostService.getHost(1L) }
            verify(exactly = 0) { discordService.sendNewEventNotification(any()) }
            assertEquals(1L, result.id)
        }

        @Test
        @DisplayName("hostName 사용 - 새 Host 생성하여 Event 생성")
        fun createEventWithHostName() {
            val host = createHost(id = 2L, name = "신규 주최")
            val savedEvent = createEvent(id = 2L, host = host)
            val eventRequest = EventCreateRequest(
                title = "테스트 행사", startAt = savedEvent.startAt, endAt = savedEvent.endAt,
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = null, hostName = "신규 주최"
            )

            every { hostService.findOrCreateHost(HostRequest(name = "신규 주최", thumbnail = null)) } returns host
            every { eventRepository.save(any<Event>()) } returns savedEvent
            every { viewService.createView(any()) } returns mockk<View>(relaxed = true)

            val result = eventService.createEvent(eventRequest, null, null, true)

            verify(exactly = 1) { hostService.findOrCreateHost(HostRequest(name = "신규 주최", thumbnail = null)) }
            assertEquals(2L, result.id)
        }

        @Test
        @DisplayName("hostId, hostName 둘 다 없으면 IllegalArgumentException")
        fun createEventWithoutHostThrowsException() {
            val eventRequest = EventCreateRequest(
                title = "테스트 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = null, hostName = null
            )

            val exception = assertThrows<IllegalArgumentException> {
                eventService.createEvent(eventRequest, null, null, true)
            }
            assertTrue(exception.message!!.contains("hostId 또는 hostName"))
        }

        @Test
        @DisplayName("autoApprove=true - status=RECRUITMENT_WAITING, statusGroup=ACTIVE")
        fun createEventApprovedTrue() {
            val host = createHost()
            val savedEvent = createEvent(host = host)
            val eventRequest = EventCreateRequest(
                title = "테스트 행사", startAt = savedEvent.startAt, endAt = savedEvent.endAt,
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, hostName = null
            )
            val eventSlot = slot<Event>()

            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(capture(eventSlot)) } returns savedEvent
            every { viewService.createView(any()) } returns mockk<View>(relaxed = true)

            eventService.createEvent(eventRequest, null, null, true)

            val captured = eventSlot.captured
            assertEquals(EventStatus.RECRUITMENT_WAITING, captured.status)
            assertEquals(EventStatusGroup.ACTIVE, captured.statusGroup)
            verify(exactly = 0) { discordService.sendNewEventNotification(any()) }
        }

        @Test
        @DisplayName("autoApprove=false - status=PENDING, statusGroup=PENDING, Discord 호출")
        fun createEventApprovedFalse() {
            val host = createHost()
            val savedEvent = createEvent(
                host = host,
                status = EventStatus.PENDING, statusGroup = EventStatusGroup.PENDING
            )
            val eventRequest = EventCreateRequest(
                title = "테스트 행사", startAt = savedEvent.startAt, endAt = savedEvent.endAt,
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, hostName = null
            )
            val eventSlot = slot<Event>()

            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(capture(eventSlot)) } returns savedEvent
            every { viewService.createView(any()) } returns mockk<View>(relaxed = true)
            every { discordService.sendNewEventNotification(any()) } returns Unit

            eventService.createEvent(eventRequest, null, null, false)

            val captured = eventSlot.captured
            assertEquals(EventStatus.PENDING, captured.status)
            assertEquals(EventStatusGroup.PENDING, captured.statusGroup)
            verify(exactly = 1) { discordService.sendNewEventNotification(any()) }
        }

        @Test
        @DisplayName("eventThumbnail 있으면 uploadFile 호출")
        fun createEventWithEventThumbnail() {
            val host = createHost()
            val savedEvent = createEvent(host = host, thumbnail = "https://storage.com/event.jpg")
            val eventRequest = EventCreateRequest(
                title = "테스트 행사", startAt = savedEvent.startAt, endAt = savedEvent.endAt,
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, hostName = null
            )
            val eventThumbnail = mockk<MultipartFile>()

            every { hostService.getHost(1L) } returns host
            every { fileStorageService.uploadFile(eventThumbnail, "events") } returns "https://storage.com/event.jpg"
            every { eventRepository.save(any<Event>()) } returns savedEvent
            every { viewService.createView(any()) } returns mockk<View>(relaxed = true)

            eventService.createEvent(eventRequest, eventThumbnail, null, true)

            verify(exactly = 1) { fileStorageService.uploadFile(eventThumbnail, "events") }
        }

        @Test
        @DisplayName("eventThumbnail 없으면 uploadFile 호출 안 함")
        fun createEventWithoutEventThumbnail() {
            val host = createHost()
            val savedEvent = createEvent(host = host)
            val eventRequest = EventCreateRequest(
                title = "테스트 행사", startAt = savedEvent.startAt, endAt = savedEvent.endAt,
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, hostName = null
            )

            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(any<Event>()) } returns savedEvent
            every { viewService.createView(any()) } returns mockk<View>(relaxed = true)

            eventService.createEvent(eventRequest, null, null, true)

            verify(exactly = 0) { fileStorageService.uploadFile(any(), any()) }
        }

        @Test
        @DisplayName("hostThumbnail 있으면 uploadFile 호출하여 Host 생성")
        fun createEventWithHostThumbnail() {
            val host = createHost(id = 2L, name = "신규 주최", thumbnail = "https://storage.com/host.jpg")
            val savedEvent = createEvent(host = host)
            val eventRequest = EventCreateRequest(
                title = "테스트 행사", startAt = savedEvent.startAt, endAt = savedEvent.endAt,
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = null, hostName = "신규 주최"
            )
            val hostThumbnail = mockk<MultipartFile>()

            every { fileStorageService.uploadFile(hostThumbnail, "hosts") } returns "https://storage.com/host.jpg"
            every {
                hostService.findOrCreateHost(HostRequest(name = "신규 주최", thumbnail = "https://storage.com/host.jpg"))
            } returns host
            every { eventRepository.save(any<Event>()) } returns savedEvent
            every { viewService.createView(any()) } returns mockk<View>(relaxed = true)

            eventService.createEvent(eventRequest, null, hostThumbnail, true)

            verify(exactly = 1) { fileStorageService.uploadFile(hostThumbnail, "hosts") }
        }
    }

    @Nested
    @DisplayName("getEvents (커서 페이지네이션)")
    inner class GetEventsTests {
        private lateinit var eventRepository: EventRepository
        private lateinit var viewService: ViewService
        private lateinit var securityUtil: SecurityUtil
        private lateinit var discordService: DiscordService
        private lateinit var hostService: HostService
        private lateinit var fileStorageService: FileStorageService
        private lateinit var eventService: EventService

        @BeforeEach
        fun setUp() {
            eventRepository = mockk()
            viewService = mockk()
            securityUtil = mockk()
            discordService = mockk()
            hostService = mockk()
            fileStorageService = mockk()
            eventService = createEventService(eventRepository, viewService, securityUtil, discordService, hostService, fileStorageService)
        }

        @Test
        @DisplayName("hasNext=true - events.size > param.size이면 nextCursor 존재")
        fun getEventsHasNextTrue() {
            val param = EventCursorPaginationParam(size = 2, statusGroup = EventStatusGroup.ACTIVE)
            val host = createHost()
            val events = (1L..3L).map { i -> createEvent(id = i, host = host, title = "행사$i") }

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventRepository.findEvents(param, null) } returns events

            val result = eventService.getEvents(param)

            assertTrue(result.pageInfo.hasNext)
            assertNotNull(result.pageInfo.nextCursor)
            assertEquals(2, result.content.size)
            assertEquals(1L, result.content[0].id)
            assertEquals(2L, result.content[1].id)
        }

        @Test
        @DisplayName("hasNext=false - events.size <= param.size이면 nextCursor null")
        fun getEventsHasNextFalse() {
            val param = EventCursorPaginationParam(size = 5, statusGroup = EventStatusGroup.ACTIVE)
            val host = createHost()
            val events = listOf(createEvent(id = 1L, host = host))

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventRepository.findEvents(param, null) } returns events

            val result = eventService.getEvents(param)

            assertFalse(result.pageInfo.hasNext)
            assertNull(result.pageInfo.nextCursor)
            assertEquals(1, result.content.size)
        }

        @Test
        @DisplayName("currentUserId != null && actualEvents.isNotEmpty() - 북마크 정보 포함")
        fun getEventsWithBookmark() {
            val param = EventCursorPaginationParam(size = 10, statusGroup = EventStatusGroup.ACTIVE)
            val host = createHost()
            val events = listOf(
                createEvent(id = 1L, host = host, title = "행사1"),
                createEvent(id = 2L, host = host, title = "행사2"),
            )

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventRepository.findEvents(param, 100L) } returns events
            every { eventRepository.findBookmarkedEventIds(100L, listOf(1L, 2L)) } returns listOf(1L)

            val result = eventService.getEvents(param)

            assertEquals(2, result.content.size)
            assertTrue(result.content[0].isBookmarked)
            assertFalse(result.content[1].isBookmarked)
        }

        @Test
        @DisplayName("currentUserId == null - 북마크 정보 없이 반환")
        fun getEventsWithoutBookmark() {
            val param = EventCursorPaginationParam(size = 10, statusGroup = EventStatusGroup.ACTIVE)
            val host = createHost()
            val events = listOf(createEvent(id = 1L, host = host))

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventRepository.findEvents(param, null) } returns events

            val result = eventService.getEvents(param)

            assertEquals(1, result.content.size)
            assertFalse(result.content[0].isBookmarked)
            verify(exactly = 0) { eventRepository.findBookmarkedEventIds(any(), any()) }
        }

        @Test
        @DisplayName("currentUserId != null && actualEvents.isEmpty() - 북마크 조회 안 함")
        fun getEventsEmptyList() {
            val param = EventCursorPaginationParam(size = 10, statusGroup = EventStatusGroup.ACTIVE)

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventRepository.findEvents(param, 100L) } returns emptyList()

            val result = eventService.getEvents(param)

            assertEquals(0, result.content.size)
            verify(exactly = 0) { eventRepository.findBookmarkedEventIds(any(), any()) }
        }
    }

    @Nested
    @DisplayName("getEventDetail")
    inner class GetEventDetailTests {
        private lateinit var eventRepository: EventRepository
        private lateinit var viewService: ViewService
        private lateinit var securityUtil: SecurityUtil
        private lateinit var discordService: DiscordService
        private lateinit var hostService: HostService
        private lateinit var fileStorageService: FileStorageService
        private lateinit var eventService: EventService

        @BeforeEach
        fun setUp() {
            eventRepository = mockk()
            viewService = mockk()
            securityUtil = mockk()
            discordService = mockk()
            hostService = mockk()
            fileStorageService = mockk()
            eventService = createEventService(eventRepository, viewService, securityUtil, discordService, hostService, fileStorageService)
        }

        @Test
        @DisplayName("로그인 상태 - 북마크 정보 포함")
        fun getEventDetailWithBookmark() {
            val host = createHost()
            val event = createEvent(id = 1L, host = host)

            every { eventRepository.findById(1L) } returns Optional.of(event)
            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventRepository.findBookmarkedEventIds(100L, listOf(1L)) } returns listOf(1L)

            val result = eventService.getEventDetail(1L)

            assertTrue(result.isBookmarked)
            assertEquals(1L, result.id)
        }

        @Test
        @DisplayName("비로그인 상태 - isBookmarked=false")
        fun getEventDetailWithoutBookmark() {
            val host = createHost()
            val event = createEvent(id = 1L, host = host)

            every { eventRepository.findById(1L) } returns Optional.of(event)
            every { securityUtil.getCurrentUserIdOrNull() } returns null

            val result = eventService.getEventDetail(1L)

            assertFalse(result.isBookmarked)
            verify(exactly = 0) { eventRepository.findBookmarkedEventIds(any(), any()) }
        }
    }

    @Nested
    @DisplayName("updateEvent")
    inner class UpdateEventTests {
        private lateinit var eventRepository: EventRepository
        private lateinit var viewService: ViewService
        private lateinit var securityUtil: SecurityUtil
        private lateinit var discordService: DiscordService
        private lateinit var hostService: HostService
        private lateinit var fileStorageService: FileStorageService
        private lateinit var eventService: EventService

        @BeforeEach
        fun setUp() {
            eventRepository = mockk()
            viewService = mockk()
            securityUtil = mockk()
            discordService = mockk()
            hostService = mockk()
            fileStorageService = mockk()
            eventService = createEventService(eventRepository, viewService, securityUtil, discordService, hostService, fileStorageService)
        }

        @Test
        @DisplayName("deleteEventThumbnail=true - 기존 썸네일 삭제")
        fun updateEventDeleteThumbnail() {
            val host = createHost()
            val event = createEvent(host = host, thumbnail = "https://storage.com/old.jpg")
            val updateRequest = EventUpdateRequest(
                title = "수정된 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, deleteEventThumbnail = true
            )

            every { eventRepository.findById(1L) } returns Optional.of(event)
            every { fileStorageService.deleteFile("https://storage.com/old.jpg") } returns true
            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(event) } returns event

            eventService.updateEvent(1L, updateRequest, null, null)

            verify(exactly = 1) { fileStorageService.deleteFile("https://storage.com/old.jpg") }
            assertNull(event.thumbnail, "썸네일이 null로 설정되어야 합니다")
        }

        @Test
        @DisplayName("eventThumbnail != null - 기존 삭제 후 새거 업로드")
        fun updateEventWithNewThumbnail() {
            val host = createHost()
            val event = createEvent(host = host, thumbnail = "https://storage.com/old.jpg")
            val updateRequest = EventUpdateRequest(
                title = "수정된 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, deleteEventThumbnail = false
            )
            val newThumbnail = mockk<MultipartFile>()

            every { eventRepository.findById(1L) } returns Optional.of(event)
            every { fileStorageService.deleteFile("https://storage.com/old.jpg") } returns true
            every { fileStorageService.uploadFile(newThumbnail, "events") } returns "https://storage.com/new.jpg"
            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(event) } returns event

            eventService.updateEvent(1L, updateRequest, newThumbnail, null)

            verify(exactly = 1) { fileStorageService.deleteFile("https://storage.com/old.jpg") }
            verify(exactly = 1) { fileStorageService.uploadFile(newThumbnail, "events") }
        }

        @Test
        @DisplayName("썸네일 변경 없음 - 기존 유지")
        fun updateEventKeepThumbnail() {
            val host = createHost()
            val event = createEvent(host = host, thumbnail = "https://storage.com/old.jpg")
            val updateRequest = EventUpdateRequest(
                title = "수정된 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, deleteEventThumbnail = false
            )

            every { eventRepository.findById(1L) } returns Optional.of(event)
            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(event) } returns event

            eventService.updateEvent(1L, updateRequest, null, null)

            verify(exactly = 0) { fileStorageService.deleteFile(any()) }
            verify(exactly = 0) { fileStorageService.uploadFile(any(), any()) }
        }

        @Test
        @DisplayName("hostId 있음 - 기존 Host 사용")
        fun updateEventWithHostId() {
            val newHost = createHost(id = 2L, name = "변경된 주최")
            val event = createEvent(host = createHost())
            val updateRequest = EventUpdateRequest(
                title = "수정된 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 2L, hostName = null
            )

            every { eventRepository.findById(1L) } returns Optional.of(event)
            every { hostService.getHost(2L) } returns newHost
            every { eventRepository.save(event) } returns event

            eventService.updateEvent(1L, updateRequest, null, null)

            verify(exactly = 1) { hostService.getHost(2L) }
            verify(exactly = 0) { hostService.findOrCreateHost(any()) }
        }

        @Test
        @DisplayName("hostName + deleteHostThumbnail=true - 썸네일 null로 Host 생성")
        fun updateEventWithHostNameDeleteThumbnail() {
            val newHost = createHost(id = 2L, name = "신규 주최")
            val event = createEvent(host = createHost(thumbnail = "https://storage.com/oldhost.jpg"))
            val updateRequest = EventUpdateRequest(
                title = "수정된 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = null, hostName = "신규 주최", deleteHostThumbnail = true
            )

            every { eventRepository.findById(1L) } returns Optional.of(event)
            every { hostService.findOrCreateHost(HostRequest(name = "신규 주최", thumbnail = null)) } returns newHost
            every { eventRepository.save(event) } returns event

            eventService.updateEvent(1L, updateRequest, null, null)

            verify(exactly = 1) { hostService.findOrCreateHost(HostRequest(name = "신규 주최", thumbnail = null)) }
        }

        @Test
        @DisplayName("hostName + hostThumbnail - 새 썸네일로 Host 생성")
        fun updateEventWithHostThumbnail() {
            val newHost = createHost(id = 2L, name = "신규 주최", thumbnail = "https://storage.com/newhost.jpg")
            val event = createEvent(host = createHost())
            val updateRequest = EventUpdateRequest(
                title = "수정된 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = null, hostName = "신규 주최"
            )
            val hostThumbnail = mockk<MultipartFile>()

            every { eventRepository.findById(1L) } returns Optional.of(event)
            every { fileStorageService.uploadFile(hostThumbnail, "hosts") } returns "https://storage.com/newhost.jpg"
            every {
                hostService.findOrCreateHost(HostRequest(name = "신규 주최", thumbnail = "https://storage.com/newhost.jpg"))
            } returns newHost
            every { eventRepository.save(event) } returns event

            eventService.updateEvent(1L, updateRequest, null, hostThumbnail)

            verify(exactly = 1) { fileStorageService.uploadFile(hostThumbnail, "hosts") }
        }

        @Test
        @DisplayName("hostName + 썸네일 변경 없음 - 기존 Host 썸네일 유지")
        fun updateEventKeepHostThumbnail() {
            val oldHost = createHost(thumbnail = "https://storage.com/oldhost.jpg")
            val newHost = createHost(id = 2L, name = "신규 주최", thumbnail = "https://storage.com/oldhost.jpg")
            val event = createEvent(host = oldHost)
            val updateRequest = EventUpdateRequest(
                title = "수정된 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = null, hostName = "신규 주최", deleteHostThumbnail = false
            )

            every { eventRepository.findById(1L) } returns Optional.of(event)
            every {
                hostService.findOrCreateHost(HostRequest(name = "신규 주최", thumbnail = "https://storage.com/oldhost.jpg"))
            } returns newHost
            every { eventRepository.save(event) } returns event

            eventService.updateEvent(1L, updateRequest, null, null)

            verify(exactly = 1) {
                hostService.findOrCreateHost(HostRequest(name = "신규 주최", thumbnail = "https://storage.com/oldhost.jpg"))
            }
        }

        @Test
        @DisplayName("hostId, hostName 둘 다 없으면 IllegalArgumentException")
        fun updateEventWithoutHostThrowsException() {
            val event = createEvent(host = createHost())
            val updateRequest = EventUpdateRequest(
                title = "수정된 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = null, hostName = null
            )

            every { eventRepository.findById(1L) } returns Optional.of(event)

            val exception = assertThrows<IllegalArgumentException> {
                eventService.updateEvent(1L, updateRequest, null, null)
            }
            assertTrue(exception.message!!.contains("hostId 또는 hostName"))
        }
    }

    @Nested
    @DisplayName("deleteEvents")
    inner class DeleteEventsTests {
        private lateinit var eventRepository: EventRepository
        private lateinit var viewService: ViewService
        private lateinit var securityUtil: SecurityUtil
        private lateinit var discordService: DiscordService
        private lateinit var hostService: HostService
        private lateinit var fileStorageService: FileStorageService
        private lateinit var eventService: EventService

        @BeforeEach
        fun setUp() {
            eventRepository = mockk()
            viewService = mockk()
            securityUtil = mockk()
            discordService = mockk()
            hostService = mockk()
            fileStorageService = mockk()
            eventService = createEventService(eventRepository, viewService, securityUtil, discordService, hostService, fileStorageService)
        }

        @Test
        @DisplayName("썸네일 있는 이벤트들 - deleteFile 호출 후 deleteAllById")
        fun deleteEventsWithThumbnails() {
            val eventIds = listOf(1L, 2L, 3L)
            val host = createHost()
            val eventsWithThumbnails = listOf(
                createEvent(id = 1L, host = host, thumbnail = "https://storage.com/1.jpg"),
                createEvent(id = 2L, host = host, thumbnail = "https://storage.com/2.jpg"),
            )

            every { eventRepository.findAllByIdInAndThumbnailNotNull(eventIds) } returns eventsWithThumbnails
            every { fileStorageService.deleteFile(any()) } returns true
            every { eventRepository.deleteAllById(eventIds) } returns Unit

            eventService.deleteEvents(eventIds)

            verify(exactly = 1) { fileStorageService.deleteFile("https://storage.com/1.jpg") }
            verify(exactly = 1) { fileStorageService.deleteFile("https://storage.com/2.jpg") }
            verify(exactly = 1) { eventRepository.deleteAllById(eventIds) }
        }

        @Test
        @DisplayName("썸네일 없는 이벤트들 - deleteFile 호출 안 함")
        fun deleteEventsWithoutThumbnails() {
            val eventIds = listOf(1L, 2L)

            every { eventRepository.findAllByIdInAndThumbnailNotNull(eventIds) } returns emptyList()
            every { eventRepository.deleteAllById(eventIds) } returns Unit

            eventService.deleteEvents(eventIds)

            verify(exactly = 0) { fileStorageService.deleteFile(any()) }
            verify(exactly = 1) { eventRepository.deleteAllById(eventIds) }
        }
    }

    @Nested
    @DisplayName("updateStatus")
    inner class UpdateStatusTests {
        private lateinit var eventRepository: EventRepository
        private lateinit var viewService: ViewService
        private lateinit var securityUtil: SecurityUtil
        private lateinit var discordService: DiscordService
        private lateinit var hostService: HostService
        private lateinit var fileStorageService: FileStorageService
        private lateinit var eventService: EventService

        @BeforeEach
        fun setUp() {
            eventRepository = mockk()
            viewService = mockk()
            securityUtil = mockk()
            discordService = mockk()
            hostService = mockk()
            fileStorageService = mockk()
            eventService = createEventService(eventRepository, viewService, securityUtil, discordService, hostService, fileStorageService)
        }

        @Test
        @DisplayName("승인 처리 - status 변경 및 updateStatus 호출")
        fun updateStatusSuccess() {
            val host = createHost()
            val event = createEvent(
                host = host,
                status = EventStatus.PENDING, statusGroup = EventStatusGroup.PENDING,
                recruitmentStartAt = LocalDateTime.now().plusDays(1),
            )

            every { eventRepository.findById(1L) } returns Optional.of(event)

            eventService.updateStatus(1L)

            assertEquals(EventStatus.RECRUITMENT_WAITING, event.status)
            assertEquals(EventStatusGroup.ACTIVE, event.statusGroup)
        }
    }

    @Nested
    @DisplayName("getEvents4Calendar")
    inner class GetEvents4CalendarTests {
        private lateinit var eventRepository: EventRepository
        private lateinit var viewService: ViewService
        private lateinit var securityUtil: SecurityUtil
        private lateinit var discordService: DiscordService
        private lateinit var hostService: HostService
        private lateinit var fileStorageService: FileStorageService
        private lateinit var eventService: EventService

        @BeforeEach
        fun setUp() {
            eventRepository = mockk()
            viewService = mockk()
            securityUtil = mockk()
            discordService = mockk()
            hostService = mockk()
            fileStorageService = mockk()
            eventService = createEventService(eventRepository, viewService, securityUtil, discordService, hostService, fileStorageService)
        }

        @Test
        @DisplayName("getCurrentUserId 호출하여 모든 결과에 isBookmarked=true")
        fun getEvents4CalendarSuccess() {
            val request = Event4CalendarRequest(month = 2, year = 2026, type = null)
            val host = createHost()
            val events = listOf(
                createEvent(id = 1L, host = host, startAt = LocalDateTime.of(2026, 2, 15, 9, 0)),
                createEvent(id = 2L, host = host, startAt = LocalDateTime.of(2026, 2, 20, 10, 0)),
            )

            every { securityUtil.getCurrentUserId() } returns 100L
            every {
                eventRepository.findEvents4Calendar(
                    100L,
                    LocalDateTime.of(2026, 2, 1, 0, 0),
                    LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999999999),
                    null
                )
            } returns events

            val result = eventService.getEvents4Calendar(request)

            assertEquals(2, result.size)
            assertTrue(result[0].isBookmarked)
            assertTrue(result[1].isBookmarked)
        }

        @Test
        @DisplayName("특정 EventType으로 필터링")
        fun getEvents4CalendarWithType() {
            val request = Event4CalendarRequest(month = 2, year = 2026, type = EventType.CONFERENCE)
            val host = createHost()
            val events = listOf(
                createEvent(id = 1L, host = host, startAt = LocalDateTime.of(2026, 2, 15, 9, 0)),
            )

            every { securityUtil.getCurrentUserId() } returns 100L
            every {
                eventRepository.findEvents4Calendar(
                    100L,
                    LocalDateTime.of(2026, 2, 1, 0, 0),
                    LocalDateTime.of(2026, 2, 28, 23, 59, 59, 999999999),
                    EventType.CONFERENCE
                )
            } returns events

            val result = eventService.getEvents4Calendar(request)

            assertEquals(1, result.size)
            assertTrue(result[0].isBookmarked)
        }
    }

    @Nested
    @DisplayName("countActiveEvents")
    inner class CountActiveEventsTests {
        private lateinit var eventRepository: EventRepository
        private lateinit var viewService: ViewService
        private lateinit var securityUtil: SecurityUtil
        private lateinit var discordService: DiscordService
        private lateinit var hostService: HostService
        private lateinit var fileStorageService: FileStorageService
        private lateinit var eventService: EventService

        @BeforeEach
        fun setUp() {
            eventRepository = mockk()
            viewService = mockk()
            securityUtil = mockk()
            discordService = mockk()
            hostService = mockk()
            fileStorageService = mockk()
            eventService = createEventService(eventRepository, viewService, securityUtil, discordService, hostService, fileStorageService)
        }

        @Test
        @DisplayName("eventRepository.countActiveEvents() 위임 확인")
        fun countActiveEventsSuccess() {
            every { eventRepository.countActiveEvents() } returns 42L

            val result = eventService.countActiveEvents()

            assertEquals(42L, result)
            verify(exactly = 1) { eventRepository.countActiveEvents() }
        }
    }
}
