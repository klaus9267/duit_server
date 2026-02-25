package duit.server.domain.event.service

import duit.server.domain.common.dto.pagination.CursorPageInfo
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.dto.EventResponseV2
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.dto.HostResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.time.LocalDateTime

@DisplayName("EventCacheService 단위 테스트")
class EventCacheServiceTest {

    private lateinit var redisTemplate: RedisTemplate<String, Any>
    private lateinit var valueOps: ValueOperations<String, Any>
    private lateinit var eventCacheService: EventCacheService

    @BeforeEach
    fun setUp() {
        redisTemplate = mockk()
        valueOps = mockk()
        every { redisTemplate.opsForValue() } returns valueOps
        eventCacheService = EventCacheService(redisTemplate)
    }

    private fun createEventResponse(id: Long, isBookmarked: Boolean = false) = EventResponseV2(
        id = id,
        title = "행사$id",
        startAt = LocalDateTime.of(2026, 3, 1, 9, 0),
        endAt = LocalDateTime.of(2026, 3, 2, 18, 0),
        recruitmentStartAt = null,
        recruitmentEndAt = null,
        uri = "https://example.com/$id",
        thumbnail = null,
        eventType = EventType.CONFERENCE,
        eventStatus = EventStatus.RECRUITING,
        eventStatusGroup = EventStatusGroup.ACTIVE,
        host = HostResponse(id = 1L, name = "테스트 주최", thumbnail = null),
        viewCount = 0,
        isBookmarked = isBookmarked
    )

    private fun createCacheResponse(vararg ids: Long) = CursorPageResponse(
        content = ids.map { createEventResponse(it) },
        pageInfo = CursorPageInfo(hasNext = false, nextCursor = null, pageSize = ids.size)
    )

    @Nested
    @DisplayName("isCacheable")
    inner class IsCacheableTests {

        @Test
        @DisplayName("기본 조건 - 캐싱 가능")
        fun `기본 파라미터는 캐싱 가능`() {
            val param = EventCursorPaginationParam(size = 10)

            assertTrue(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("VIEW_COUNT 정렬 - 캐싱 불가")
        fun `VIEW_COUNT 정렬은 캐싱하지 않음`() {
            val param = EventCursorPaginationParam(field = PaginationField.VIEW_COUNT)

            assertFalse(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("bookmarked=true - 캐싱 불가")
        fun `북마크 필터는 캐싱하지 않음`() {
            val param = EventCursorPaginationParam(bookmarked = true)

            assertFalse(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("searchKeyword 있음 - 캐싱 불가")
        fun `검색 키워드가 있으면 캐싱하지 않음`() {
            val param = EventCursorPaginationParam(searchKeyword = "간호")

            assertFalse(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("hostId 있음 - 캐싱 불가")
        fun `hostId가 있으면 캐싱하지 않음`() {
            val param = EventCursorPaginationParam(hostId = 1L)

            assertFalse(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("CREATED_AT 정렬 - 캐싱 가능")
        fun `CREATED_AT 정렬은 캐싱 가능`() {
            val param = EventCursorPaginationParam(field = PaginationField.CREATED_AT)

            assertTrue(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("START_DATE 정렬 - 캐싱 가능")
        fun `START_DATE 정렬은 캐싱 가능`() {
            val param = EventCursorPaginationParam(field = PaginationField.START_DATE)

            assertTrue(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("RECRUITMENT_DEADLINE 정렬 - 캐싱 가능")
        fun `RECRUITMENT_DEADLINE 정렬은 캐싱 가능`() {
            val param = EventCursorPaginationParam(field = PaginationField.RECRUITMENT_DEADLINE)

            assertTrue(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("ID 정렬 - 캐싱 가능")
        fun `ID 정렬은 캐싱 가능`() {
            val param = EventCursorPaginationParam(field = PaginationField.ID)

            assertTrue(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("캐싱 불가 조건 복합 - bookmarked + searchKeyword")
        fun `복합 캐싱 불가 조건`() {
            val param = EventCursorPaginationParam(bookmarked = true, searchKeyword = "간호")

            assertFalse(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("types 필터만 있으면 캐싱 가능")
        fun `types 필터만 있으면 캐싱 가능`() {
            val param = EventCursorPaginationParam(types = listOf(EventType.CONFERENCE, EventType.SEMINAR))

            assertTrue(eventCacheService.isCacheable(param))
        }

        @Test
        @DisplayName("status 필터만 있으면 캐싱 가능")
        fun `status 필터만 있으면 캐싱 가능`() {
            val param = EventCursorPaginationParam(status = EventStatus.RECRUITING, statusGroup = null)

            assertTrue(eventCacheService.isCacheable(param))
        }
    }

    @Nested
    @DisplayName("getFromCache")
    inner class GetFromCacheTests {

        @Test
        @DisplayName("캐시 히트 - JSON을 역직렬화하여 반환")
        fun `캐시에 데이터가 있으면 역직렬화하여 반환`() {
            val param = EventCursorPaginationParam(size = 10)
            val response = createCacheResponse(1L, 2L)
            val json = duit.server.application.config.CacheConfig.createCacheObjectMapper()
                .writeValueAsString(response)

            every { valueOps.get(any<String>()) } answers {
                val key = firstArg<String>()
                if (key == "duit:events:v2:version") "0" else json
            }

            val result = eventCacheService.getFromCache(param)

            assertNotNull(result)
            assertEquals(2, result!!.content.size)
            assertEquals(1L, result.content[0].id)
            assertEquals(2L, result.content[1].id)
        }

        @Test
        @DisplayName("캐시 미스 - null 반환")
        fun `캐시에 데이터가 없으면 null 반환`() {
            val param = EventCursorPaginationParam(size = 10)

            every { valueOps.get(any<String>()) } returns null

            val result = eventCacheService.getFromCache(param)

            assertNull(result)
        }

        @Test
        @DisplayName("Redis 장애 시 null 반환 (서비스 중단 없음)")
        fun `Redis 장애 시 null 반환`() {
            val param = EventCursorPaginationParam(size = 10)

            every { valueOps.get(any<String>()) } throws RuntimeException("Redis connection refused")

            val result = eventCacheService.getFromCache(param)

            assertNull(result)
        }

        @Test
        @DisplayName("역직렬화 실패 시 null 반환")
        fun `잘못된 JSON이면 null 반환`() {
            val param = EventCursorPaginationParam(size = 10)

            every { valueOps.get(any<String>()) } returnsMany listOf("0", "invalid json{{{")

            val result = eventCacheService.getFromCache(param)

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("putToCache")
    inner class PutToCacheTests {

        @Test
        @DisplayName("CREATED_AT - TTL 5분으로 저장")
        fun `CREATED_AT 정렬은 5분 TTL로 저장`() {
            val param = EventCursorPaginationParam(field = PaginationField.CREATED_AT)
            val response = createCacheResponse(1L)

            every { valueOps.get(any<String>()) } returns "0"  // version
            every { valueOps.set(any(), any(), any<Duration>()) } returns Unit

            eventCacheService.putToCache(param, response)

            verify { valueOps.set(any(), any(), Duration.ofMinutes(5)) }
        }

        @Test
        @DisplayName("START_DATE - TTL 3분으로 저장")
        fun `START_DATE 정렬은 3분 TTL로 저장`() {
            val param = EventCursorPaginationParam(field = PaginationField.START_DATE)
            val response = createCacheResponse(1L)

            every { valueOps.get(any<String>()) } returns "0"  // version
            every { valueOps.set(any(), any(), any<Duration>()) } returns Unit

            eventCacheService.putToCache(param, response)

            verify { valueOps.set(any(), any(), Duration.ofMinutes(3)) }
        }

        @Test
        @DisplayName("RECRUITMENT_DEADLINE - TTL 3분으로 저장")
        fun `RECRUITMENT_DEADLINE 정렬은 3분 TTL로 저장`() {
            val param = EventCursorPaginationParam(field = PaginationField.RECRUITMENT_DEADLINE)
            val response = createCacheResponse(1L)

            every { valueOps.get(any<String>()) } returns "0"  // version
            every { valueOps.set(any(), any(), any<Duration>()) } returns Unit

            eventCacheService.putToCache(param, response)

            verify { valueOps.set(any(), any(), Duration.ofMinutes(3)) }
        }

        @Test
        @DisplayName("Redis 장애 시 예외 전파 없음")
        fun `Redis 장애 시 예외를 삼키고 정상 반환`() {
            val param = EventCursorPaginationParam(field = PaginationField.CREATED_AT)
            val response = createCacheResponse(1L)

            every { valueOps.get(any<String>()) } returns "0"
            every { valueOps.set(any(), any(), any<Duration>()) } throws RuntimeException("Redis write failed")

            // 예외 전파 없이 정상 종료
            eventCacheService.putToCache(param, response)
        }
    }

    @Nested
    @DisplayName("incrementVersion")
    inner class IncrementVersionTests {

        @Test
        @DisplayName("버전 카운터 증가 호출")
        fun `버전 카운터를 increment 함`() {
            every { valueOps.increment("duit:events:v2:version") } returns 2L

            eventCacheService.incrementVersion()

            verify(exactly = 1) { valueOps.increment("duit:events:v2:version") }
        }

        @Test
        @DisplayName("Redis 장애 시 예외 전파 없음")
        fun `Redis 장애 시 예외를 삼키고 정상 반환`() {
            every { valueOps.increment(any<String>()) } throws RuntimeException("Redis connection lost")

            // 예외 전파 없이 정상 종료
            eventCacheService.incrementVersion()
        }
    }

    @Nested
    @DisplayName("캐시 키 구조 검증")
    inner class CacheKeyTests {

        @Test
        @DisplayName("같은 파라미터 + 같은 버전 = 같은 캐시 키")
        fun `동일 조건이면 동일 캐시 키 생성`() {
            val param = EventCursorPaginationParam(size = 10)
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            every { valueOps.get(any<String>()) } returns "5"
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit

            eventCacheService.putToCache(param, response)
            eventCacheService.putToCache(param, response)

            assertEquals(2, capturedKeys.size)
            assertEquals(capturedKeys[0], capturedKeys[1])
        }

        @Test
        @DisplayName("버전이 다르면 다른 캐시 키")
        fun `버전이 변경되면 다른 캐시 키 생성`() {
            val param = EventCursorPaginationParam(size = 10)
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            // 첫 번째: version 5
            every { valueOps.get(any<String>()) } returns "5"
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit
            eventCacheService.putToCache(param, response)

            // 두 번째: version 6
            every { valueOps.get(any<String>()) } returns "6"
            eventCacheService.putToCache(param, response)

            assertEquals(2, capturedKeys.size)
            assertTrue(capturedKeys[0] != capturedKeys[1], "버전이 다르면 키도 달라야 함")
            assertTrue(capturedKeys[0].contains(":5:"), "첫 번째 키에 버전 5 포함")
            assertTrue(capturedKeys[1].contains(":6:"), "두 번째 키에 버전 6 포함")
        }

        @Test
        @DisplayName("types 필터 순서와 무관하게 같은 캐시 키")
        fun `types 정렬 순서가 달라도 같은 캐시 키`() {
            val param1 = EventCursorPaginationParam(
                size = 10,
                types = listOf(EventType.SEMINAR, EventType.CONFERENCE)
            )
            val param2 = EventCursorPaginationParam(
                size = 10,
                types = listOf(EventType.CONFERENCE, EventType.SEMINAR)
            )
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            every { valueOps.get(any<String>()) } returns "0"
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit

            eventCacheService.putToCache(param1, response)
            eventCacheService.putToCache(param2, response)

            assertEquals(capturedKeys[0], capturedKeys[1], "types 순서와 무관하게 같은 키")
        }

        @Test
        @DisplayName("size가 다르면 다른 캐시 키")
        fun `size가 다르면 다른 캐시 키`() {
            val param1 = EventCursorPaginationParam(size = 10)
            val param2 = EventCursorPaginationParam(size = 20)
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            every { valueOps.get(any<String>()) } returns "0"
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit

            eventCacheService.putToCache(param1, response)
            eventCacheService.putToCache(param2, response)

            assertTrue(capturedKeys[0] != capturedKeys[1], "size가 다르면 키도 달라야 함")
        }

        @Test
        @DisplayName("cursor가 null이면 'first'로 키 생성")
        fun `첫 페이지 cursor는 first로 키 생성`() {
            val param = EventCursorPaginationParam(size = 10, cursor = null)
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            every { valueOps.get(any<String>()) } returns "0"
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit

            eventCacheService.putToCache(param, response)

            assertTrue(capturedKeys[0].endsWith(":first"), "cursor가 null이면 키가 :first로 끝나야 함")
        }

        @Test
        @DisplayName("statusGroup ACTIVE 기본값 - 키에 sg:active 포함")
        fun `statusGroup 기본값 ACTIVE가 키에 반영`() {
            val param = EventCursorPaginationParam(size = 10)
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            every { valueOps.get(any<String>()) } returns "0"
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit

            eventCacheService.putToCache(param, response)

            assertTrue(capturedKeys[0].contains("sg:active"), "기본 statusGroup은 sg:active")
        }

        @Test
        @DisplayName("status 필터 사용 시 키에 s:status_name 포함")
        fun `status 필터가 키에 반영`() {
            val param = EventCursorPaginationParam(
                size = 10,
                status = EventStatus.RECRUITING,
                statusGroup = null
            )
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            every { valueOps.get(any<String>()) } returns "0"
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit

            eventCacheService.putToCache(param, response)

            assertTrue(capturedKeys[0].contains("s:recruiting"), "status 필터가 키에 반영되어야 함")
        }
    }
}
