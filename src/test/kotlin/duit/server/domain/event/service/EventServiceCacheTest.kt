package duit.server.domain.event.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.common.dto.pagination.CursorPageInfo
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.dto.EventResponseV2
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.entity.Host
import duit.server.domain.host.service.HostService
import duit.server.infrastructure.external.discord.DiscordService
import duit.server.infrastructure.external.file.FileStorageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("EventService 캐시 연동 테스트")
class EventServiceCacheTest {

    private lateinit var eventRepository: EventRepository
    private lateinit var securityUtil: SecurityUtil
    private lateinit var discordService: DiscordService
    private lateinit var hostService: HostService
    private lateinit var fileStorageService: FileStorageService
    private lateinit var eventCacheService: EventCacheService
    private lateinit var eventService: EventService

    @BeforeEach
    fun setUp() {
        eventRepository = mockk()
        securityUtil = mockk()
        discordService = mockk()
        hostService = mockk()
        fileStorageService = mockk()
        eventCacheService = mockk(relaxed = true)
        eventService = EventService(
            eventRepository, securityUtil, discordService,
            hostService, fileStorageService, eventCacheService
        )
    }

    private fun createHost(id: Long = 1L) = Host(id = id, name = "테스트 주최", thumbnail = null)

    private fun createEvent(id: Long, host: Host) = Event(
        id = id, title = "행사$id",
        startAt = LocalDateTime.now().plusDays(7),
        endAt = LocalDateTime.now().plusDays(8),
        recruitmentStartAt = null, recruitmentEndAt = null,
        uri = "https://example.com/$id", thumbnail = null,
        eventType = EventType.CONFERENCE, host = host,
        status = EventStatus.RECRUITING, statusGroup = EventStatusGroup.ACTIVE
    )

    private fun createCachedResponse(vararg ids: Long, isBookmarked: Boolean = false): CursorPageResponse<EventResponseV2> {
        val host = createHost()
        return CursorPageResponse(
            content = ids.map { id ->
                EventResponseV2.from(createEvent(id, host), isBookmarked)
            },
            pageInfo = CursorPageInfo(hasNext = false, nextCursor = null, pageSize = ids.size)
        )
    }

    @Nested
    @DisplayName("캐시 히트 경로")
    inner class CacheHitTests {

        @Test
        @DisplayName("캐시 히트 시 DB 조회하지 않음")
        fun `캐시 히트하면 DB를 조회하지 않는다`() {
            val param = EventCursorPaginationParam(size = 10)
            val cached = createCachedResponse(1L, 2L)

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns cached

            val result = eventService.getEvents(param)

            assertEquals(2, result.content.size)
            verify(exactly = 0) { eventRepository.findEvents(any(), any()) }
        }

        @Test
        @DisplayName("캐시 히트 + 로그인 유저 - 북마크 오버레이")
        fun `캐시 히트 시 로그인 유저의 북마크를 오버레이한다`() {
            val param = EventCursorPaginationParam(size = 10)
            val cached = createCachedResponse(1L, 2L, 3L, isBookmarked = false)

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns cached
            every { eventRepository.findBookmarkedEventIds(100L, listOf(1L, 2L, 3L)) } returns listOf(1L, 3L)

            val result = eventService.getEvents(param)

            assertTrue(result.content[0].isBookmarked, "ID 1은 북마크됨")
            assertFalse(result.content[1].isBookmarked, "ID 2는 북마크 안 됨")
            assertTrue(result.content[2].isBookmarked, "ID 3은 북마크됨")
            // DB 행사 조회는 하지 않고 북마크 조회만 함
            verify(exactly = 0) { eventRepository.findEvents(any(), any()) }
            verify(exactly = 1) { eventRepository.findBookmarkedEventIds(100L, any()) }
        }

        @Test
        @DisplayName("캐시 히트 + 비로그인 유저 - 북마크 오버레이 없이 그대로 반환")
        fun `캐시 히트 시 비로그인 유저는 캐시 그대로 반환`() {
            val param = EventCursorPaginationParam(size = 10)
            val cached = createCachedResponse(1L, 2L, isBookmarked = false)

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns cached

            val result = eventService.getEvents(param)

            assertFalse(result.content[0].isBookmarked)
            assertFalse(result.content[1].isBookmarked)
            verify(exactly = 0) { eventRepository.findBookmarkedEventIds(any(), any()) }
        }

        @Test
        @DisplayName("캐시 히트 + 빈 content - 북마크 조회 스킵")
        fun `캐시 히트지만 빈 결과면 북마크 조회하지 않음`() {
            val param = EventCursorPaginationParam(size = 10)
            val cached = CursorPageResponse<EventResponseV2>(
                content = emptyList(),
                pageInfo = CursorPageInfo(hasNext = false, nextCursor = null, pageSize = 0)
            )

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns cached

            val result = eventService.getEvents(param)

            assertEquals(0, result.content.size)
            verify(exactly = 0) { eventRepository.findBookmarkedEventIds(any(), any()) }
        }
    }

    @Nested
    @DisplayName("캐시 미스 경로")
    inner class CacheMissTests {

        @Test
        @DisplayName("캐시 미스 - DB 조회 후 캐시 저장")
        fun `캐시 미스 시 DB 조회하고 캐시에 저장한다`() {
            val param = EventCursorPaginationParam(size = 10)
            val host = createHost()
            val events = listOf(createEvent(1L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns null
            every { eventRepository.findEvents(param, null) } returns events

            val result = eventService.getEvents(param)

            assertEquals(1, result.content.size)
            verify(exactly = 1) { eventRepository.findEvents(param, null) }
            verify(exactly = 1) { eventCacheService.putToCache(param, any()) }
        }

        @Test
        @DisplayName("캐시 미스 - 저장되는 데이터에 북마크 정보 없음 (isBookmarked=false)")
        fun `캐시 저장 시 북마크 정보를 제거한다`() {
            val param = EventCursorPaginationParam(size = 10)
            val host = createHost()
            val events = listOf(createEvent(1L, host), createEvent(2L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns null
            every { eventRepository.findEvents(param, 100L) } returns events
            every { eventRepository.findBookmarkedEventIds(100L, listOf(1L, 2L)) } returns listOf(1L)

            val capturedResponses = mutableListOf<CursorPageResponse<EventResponseV2>>()
            every { eventCacheService.putToCache(param, capture(capturedResponses)) } returns Unit

            eventService.getEvents(param)

            assertEquals(1, capturedResponses.size)
            val stored = capturedResponses[0]
            assertTrue(stored.content.all { !it.isBookmarked }, "캐시에는 모든 isBookmarked가 false여야 함")
        }

        @Test
        @DisplayName("캐시 미스 - 사용자에게는 북마크 포함 응답 반환")
        fun `캐시 미스 시 사용자에게는 북마크 포함 응답 반환`() {
            val param = EventCursorPaginationParam(size = 10)
            val host = createHost()
            val events = listOf(createEvent(1L, host), createEvent(2L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns null
            every { eventRepository.findEvents(param, 100L) } returns events
            every { eventRepository.findBookmarkedEventIds(100L, listOf(1L, 2L)) } returns listOf(1L)

            val result = eventService.getEvents(param)

            assertTrue(result.content[0].isBookmarked, "ID 1은 북마크됨")
            assertFalse(result.content[1].isBookmarked, "ID 2는 북마크 안 됨")
        }
    }

    @Nested
    @DisplayName("캐싱 불가 경로")
    inner class NonCacheableTests {

        @Test
        @DisplayName("VIEW_COUNT 정렬 - 캐시 우회, DB 직행")
        fun `VIEW_COUNT는 캐시를 전혀 사용하지 않는다`() {
            val param = EventCursorPaginationParam(field = PaginationField.VIEW_COUNT, size = 10)
            val host = createHost()
            val events = listOf(createEvent(1L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventCacheService.isCacheable(param) } returns false
            every { eventRepository.findEvents(param, null) } returns events

            eventService.getEvents(param)

            verify(exactly = 0) { eventCacheService.getFromCache(any()) }
            verify(exactly = 0) { eventCacheService.putToCache(any(), any()) }
        }

        @Test
        @DisplayName("bookmarked=true - 캐시 우회")
        fun `북마크 필터링은 캐시를 사용하지 않는다`() {
            val param = EventCursorPaginationParam(bookmarked = true, size = 10)
            val host = createHost()
            val events = listOf(createEvent(1L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventCacheService.isCacheable(param) } returns false
            every { eventRepository.findEvents(param, 100L) } returns events
            every { eventRepository.findBookmarkedEventIds(100L, listOf(1L)) } returns listOf(1L)

            eventService.getEvents(param)

            verify(exactly = 0) { eventCacheService.getFromCache(any()) }
            verify(exactly = 0) { eventCacheService.putToCache(any(), any()) }
        }

        @Test
        @DisplayName("searchKeyword 있음 - 캐시 우회")
        fun `검색 키워드가 있으면 캐시를 사용하지 않는다`() {
            val param = EventCursorPaginationParam(searchKeyword = "간호", size = 10)
            val host = createHost()
            val events = listOf(createEvent(1L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventCacheService.isCacheable(param) } returns false
            every { eventRepository.findEvents(param, null) } returns events

            eventService.getEvents(param)

            verify(exactly = 0) { eventCacheService.getFromCache(any()) }
            verify(exactly = 0) { eventCacheService.putToCache(any(), any()) }
        }

        @Test
        @DisplayName("hostId 있음 - 캐시 우회")
        fun `hostId 필터가 있으면 캐시를 사용하지 않는다`() {
            val param = EventCursorPaginationParam(hostId = 5L, size = 10)
            val host = createHost()
            val events = listOf(createEvent(1L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventCacheService.isCacheable(param) } returns false
            every { eventRepository.findEvents(param, null) } returns events

            eventService.getEvents(param)

            verify(exactly = 0) { eventCacheService.getFromCache(any()) }
            verify(exactly = 0) { eventCacheService.putToCache(any(), any()) }
        }
    }

    @Nested
    @DisplayName("캐시 무효화")
    inner class CacheInvalidationTests {

        @Test
        @DisplayName("createEvent 후 캐시 무효화")
        fun `행사 생성 시 캐시를 무효화한다`() {
            val host = createHost()
            val savedEvent = createEvent(1L, host)
            val eventRequest = duit.server.domain.event.dto.EventCreateRequest(
                title = "새 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, hostName = null
            )

            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(any<Event>()) } returns savedEvent

            eventService.createEvent(eventRequest, null, null, true)

            verify(exactly = 1) { eventCacheService.incrementVersion() }
        }

        @Test
        @DisplayName("updateEvent 후 캐시 무효화")
        fun `행사 수정 시 캐시를 무효화한다`() {
            val host = createHost()
            val event = createEvent(1L, host)
            val updateRequest = duit.server.domain.event.dto.EventUpdateRequest(
                title = "수정된 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L
            )

            every { eventRepository.findById(1L) } returns java.util.Optional.of(event)
            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(event) } returns event

            eventService.updateEvent(1L, updateRequest, null, null)

            verify(exactly = 1) { eventCacheService.incrementVersion() }
        }

        @Test
        @DisplayName("deleteEvents 후 캐시 무효화")
        fun `행사 삭제 시 캐시를 무효화한다`() {
            val eventIds = listOf(1L, 2L)

            every { eventRepository.findAllByIdInAndThumbnailNotNull(eventIds) } returns emptyList()
            every { eventRepository.deleteAllById(eventIds) } returns Unit

            eventService.deleteEvents(eventIds)

            verify(exactly = 1) { eventCacheService.incrementVersion() }
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("캐싱 가능 정렬 + 비로그인 + 캐시 미스 - 북마크 조회 없이 캐시 저장")
        fun `비로그인 캐시 미스 시 북마크 조회 없이 캐시 저장`() {
            val param = EventCursorPaginationParam(size = 10)
            val host = createHost()
            val events = listOf(createEvent(1L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns null
            every { eventRepository.findEvents(param, null) } returns events

            eventService.getEvents(param)

            verify(exactly = 0) { eventRepository.findBookmarkedEventIds(any(), any()) }
            verify(exactly = 1) { eventCacheService.putToCache(param, any()) }
        }

        @Test
        @DisplayName("페이지네이션 hasNext=true - 캐시에 dropLast된 결과 저장")
        fun `hasNext일 때 마지막 요소를 제외하고 캐시 저장`() {
            val param = EventCursorPaginationParam(size = 2)
            val host = createHost()
            // size=2인데 3개 반환 → hasNext=true, 실제 결과 2개
            val events = listOf(createEvent(1L, host), createEvent(2L, host), createEvent(3L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns null
            every { eventRepository.findEvents(param, null) } returns events

            val capturedResponses = mutableListOf<CursorPageResponse<EventResponseV2>>()
            every { eventCacheService.putToCache(param, capture(capturedResponses)) } returns Unit

            val result = eventService.getEvents(param)

            // 사용자에게 2개 반환
            assertEquals(2, result.content.size)
            assertTrue(result.pageInfo.hasNext)

            // 캐시에도 2개 저장 (dropLast 적용됨)
            assertEquals(2, capturedResponses[0].content.size)
            assertEquals(1L, capturedResponses[0].content[0].id)
            assertEquals(2L, capturedResponses[0].content[1].id)
        }

        @Test
        @DisplayName("캐시 히트 시 pageInfo 그대로 유지")
        fun `캐시 히트 시 pageInfo가 변경되지 않는다`() {
            val param = EventCursorPaginationParam(size = 10)
            val cached = CursorPageResponse(
                content = listOf(
                    EventResponseV2.from(createEvent(1L, createHost()), false),
                    EventResponseV2.from(createEvent(2L, createHost()), false)
                ),
                pageInfo = CursorPageInfo(hasNext = true, nextCursor = "abc123", pageSize = 2)
            )

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventCacheService.isCacheable(param) } returns true
            every { eventCacheService.getFromCache(param) } returns cached
            every { eventRepository.findBookmarkedEventIds(100L, listOf(1L, 2L)) } returns listOf(1L)

            val result = eventService.getEvents(param)

            assertEquals(true, result.pageInfo.hasNext)
            assertEquals("abc123", result.pageInfo.nextCursor)
            assertEquals(2, result.pageInfo.pageSize)
        }

        @Test
        @DisplayName("다른 정렬 필드끼리 독립적 캐싱")
        fun `CREATED_AT와 START_DATE는 독립적으로 캐싱된다`() {
            val param1 = EventCursorPaginationParam(field = PaginationField.CREATED_AT, size = 10)
            val param2 = EventCursorPaginationParam(field = PaginationField.START_DATE, size = 10)
            val host = createHost()
            val events = listOf(createEvent(1L, host))

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventCacheService.isCacheable(any()) } returns true
            every { eventCacheService.getFromCache(any()) } returns null
            every { eventRepository.findEvents(any(), any()) } returns events

            eventService.getEvents(param1)
            eventService.getEvents(param2)

            // 각각 putToCache가 호출됨
            verify(exactly = 1) { eventCacheService.putToCache(param1, any()) }
            verify(exactly = 1) { eventCacheService.putToCache(param2, any()) }
        }
    }
}
