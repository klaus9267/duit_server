package duit.server.domain.event.service

import duit.server.application.security.SecurityUtil
import duit.server.domain.bookmark.service.BookmarkedEventIdsCache
import duit.server.domain.common.dto.pagination.CursorPageInfo
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.dto.EventCreateRequest
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.dto.EventResponseV2
import duit.server.domain.event.dto.EventUpdateRequest
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.entity.Host
import duit.server.domain.host.service.HostService
import duit.server.domain.subscription.service.SubscriptionNotificationService
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
    private lateinit var eventQueryService: EventQueryService
    private lateinit var eventCacheEvictService: EventCacheEvictService
    private lateinit var bookmarkedEventIdsCache: BookmarkedEventIdsCache
    private lateinit var eventService: EventService

    @BeforeEach
    fun setUp() {
        eventRepository = mockk()
        securityUtil = mockk()
        discordService = mockk()
        hostService = mockk()
        fileStorageService = mockk()
        eventQueryService = mockk(relaxed = true)
        eventCacheEvictService = mockk(relaxed = true)
        bookmarkedEventIdsCache = mockk()
        eventService = EventService(
            eventRepository, securityUtil, discordService,
            EventHostResolver(hostService, fileStorageService), fileStorageService, eventQueryService, eventCacheEvictService,
            mockk(relaxed = true), bookmarkedEventIdsCache
        )
    }

    private fun createHost(id: Long = 1L) = Host(id = id, name = "테스트 주최", thumbnail = null)

    private fun createEvent(id: Long, host: Host) = Event(
        id = id, title = "행사$id",
        startAt = LocalDateTime.now().plusDays(7),
        endAt = LocalDateTime.now().plusDays(8),
        recruitmentStartAt = null, recruitmentEndAt = null,
        uri = "https://example.com/$id", thumbnail = null,
        eventType = EventType.CONFERENCE, hosts = listOf(host),
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
    @DisplayName("캐시 가능 경로 (eventQueryService 위임)")
    inner class CacheablePathTests {

        @Test
        @DisplayName("캐시 가능한 파라미터 - eventQueryService 호출, DB 미호출")
        fun `캐시 가능한 파라미터는 eventQueryService를 통해 조회한다`() {
            val param = EventCursorPaginationParam(size = 10)
            val cached = createCachedResponse(1L, 2L)

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventQueryService.findEvents(param) } returns cached

            val result = eventService.getEvents(param)

            assertEquals(2, result.content.size)
            verify(exactly = 1) { eventQueryService.findEvents(param) }
            verify(exactly = 0) { eventRepository.findEvents(any(), any()) }
        }

        @Test
        @DisplayName("캐시 결과 + 로그인 유저 - 북마크 오버레이")
        fun `캐시 결과에 로그인 유저의 북마크를 오버레이한다`() {
            val param = EventCursorPaginationParam(size = 10)
            val cached = createCachedResponse(1L, 2L, 3L, isBookmarked = false)

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventQueryService.findEvents(param) } returns cached
            every { bookmarkedEventIdsCache.get(100L) } returns setOf(1L, 3L)

            val result = eventService.getEvents(param)

            assertTrue(result.content[0].isBookmarked, "ID 1은 북마크됨")
            assertFalse(result.content[1].isBookmarked, "ID 2는 북마크 안 됨")
            assertTrue(result.content[2].isBookmarked, "ID 3은 북마크됨")
            verify(exactly = 0) { eventRepository.findEvents(any(), any()) }
            verify(exactly = 1) { bookmarkedEventIdsCache.get(100L) }
        }

        @Test
        @DisplayName("캐시 결과 + 비로그인 유저 - 북마크 오버레이 없이 그대로 반환")
        fun `캐시 결과 비로그인 유저는 그대로 반환`() {
            val param = EventCursorPaginationParam(size = 10)
            val cached = createCachedResponse(1L, 2L, isBookmarked = false)

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventQueryService.findEvents(param) } returns cached

            val result = eventService.getEvents(param)

            assertFalse(result.content[0].isBookmarked)
            assertFalse(result.content[1].isBookmarked)
            verify(exactly = 0) { bookmarkedEventIdsCache.get(any()) }
        }

        @Test
        @DisplayName("캐시 결과 + 빈 content - 북마크 조회 스킵")
        fun `빈 캐시 결과면 북마크 조회하지 않음`() {
            val param = EventCursorPaginationParam(size = 10)
            val cached = CursorPageResponse<EventResponseV2>(
                content = emptyList(),
                pageInfo = CursorPageInfo(hasNext = false, nextCursor = null, pageSize = 0)
            )

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventQueryService.findEvents(param) } returns cached

            val result = eventService.getEvents(param)

            assertEquals(0, result.content.size)
            verify(exactly = 0) { bookmarkedEventIdsCache.get(any()) }
        }

        @Test
        @DisplayName("캐시 결과의 pageInfo 유지")
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
            every { eventQueryService.findEvents(param) } returns cached
            every { bookmarkedEventIdsCache.get(100L) } returns setOf(1L)

            val result = eventService.getEvents(param)

            assertEquals(true, result.pageInfo.hasNext)
            assertEquals("abc123", result.pageInfo.nextCursor)
            assertEquals(2, result.pageInfo.pageSize)
        }

        @Test
        @DisplayName("CREATED_AT과 START_DATE는 각각 독립적으로 eventQueryService에 위임")
        fun `다른 정렬 필드끼리 독립적으로 eventQueryService에 위임한다`() {
            val param1 = EventCursorPaginationParam(field = PaginationField.CREATED_AT, size = 10)
            val param2 = EventCursorPaginationParam(field = PaginationField.START_DATE, size = 10)

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventQueryService.findEvents(any()) } returns createCachedResponse(1L)

            eventService.getEvents(param1)
            eventService.getEvents(param2)

            verify(exactly = 1) { eventQueryService.findEvents(param1) }
            verify(exactly = 1) { eventQueryService.findEvents(param2) }
        }
    }

    @Nested
    @DisplayName("캐싱 불가 경로 (캐시 없이 DB 조회 — eventQueryService.findEventsUncached)")
    inner class NonCacheablePathTests {

        @Test
        @DisplayName("VIEW_COUNT 정렬 - 캐시 없는 조회")
        fun `VIEW_COUNT는 캐시를 쓰지 않는다`() {
            val param = EventCursorPaginationParam(field = PaginationField.VIEW_COUNT, size = 10)

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventQueryService.findEventsUncached(param, null) } returns createCachedResponse(1L)

            eventService.getEvents(param)

            verify(exactly = 0) { eventQueryService.findEvents(any()) }
            verify(exactly = 1) { eventQueryService.findEventsUncached(param, null) }
        }

        @Test
        @DisplayName("bookmarked=true - 사용자 ID 로 거른 뒤 북마크 표시를 덧입힌다")
        fun `북마크 필터링은 캐시를 쓰지 않는다`() {
            val param = EventCursorPaginationParam(bookmarked = true, size = 10)

            every { securityUtil.getCurrentUserIdOrNull() } returns 100L
            every { eventQueryService.findEventsUncached(param, 100L) } returns createCachedResponse(1L)
            every { bookmarkedEventIdsCache.get(100L) } returns setOf(1L)

            val result = eventService.getEvents(param)

            verify(exactly = 0) { eventQueryService.findEvents(any()) }
            verify(exactly = 1) { eventQueryService.findEventsUncached(param, 100L) }
            assertTrue(result.content.single().isBookmarked)
        }

        @Test
        @DisplayName("searchKeyword 있음 - 캐시 없는 조회")
        fun `검색 키워드가 있으면 캐시를 쓰지 않는다`() {
            val param = EventCursorPaginationParam(searchKeyword = "간호", size = 10)

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventQueryService.findEventsUncached(param, null) } returns createCachedResponse(1L)

            eventService.getEvents(param)

            verify(exactly = 0) { eventQueryService.findEvents(any()) }
            verify(exactly = 1) { eventQueryService.findEventsUncached(param, null) }
        }

        @Test
        @DisplayName("hostId 있음 - 캐시 경로(eventQueryService) 사용 (D-026)")
        fun `hostId 필터도 eventQueryService로 캐시한다`() {
            val param = EventCursorPaginationParam(hostId = 5L, size = 10)

            every { securityUtil.getCurrentUserIdOrNull() } returns null
            every { eventQueryService.findEvents(param) } returns createCachedResponse(1L)

            eventService.getEvents(param)

            verify(exactly = 1) { eventQueryService.findEvents(param) }
            verify(exactly = 0) { eventRepository.findEvents(any(), any()) }
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
            val eventRequest = EventCreateRequest(
                title = "새 행사", startAt = LocalDateTime.now().plusDays(7),
                endAt = LocalDateTime.now().plusDays(8),
                recruitmentStartAt = null, recruitmentEndAt = null,
                uri = "https://example.com", eventType = EventType.CONFERENCE,
                hostId = 1L, hostName = null
            )

            every { hostService.getHost(1L) } returns host
            every { eventRepository.save(any<Event>()) } returns savedEvent

            eventService.createEvent(eventRequest, null, null, true)

            verify(exactly = 1) { eventCacheEvictService.evictAll() }
        }

        @Test
        @DisplayName("updateEvent 후 캐시 무효화")
        fun `행사 수정 시 캐시를 무효화한다`() {
            val host = createHost()
            val event = createEvent(1L, host)
            val updateRequest = EventUpdateRequest(
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

            verify(exactly = 1) { eventCacheEvictService.evictAll() }
        }

        @Test
        @DisplayName("deleteEvents 후 캐시 무효화")
        fun `행사 삭제 시 캐시를 무효화한다`() {
            val eventIds = listOf(1L, 2L)

            every { eventRepository.findAllByIdInAndThumbnailNotNull(eventIds) } returns emptyList()
            every { eventRepository.deleteAllById(eventIds) } returns Unit

            eventService.deleteEvents(eventIds)

            verify(exactly = 1) { eventCacheEvictService.evictAll() }
        }
    }
}
