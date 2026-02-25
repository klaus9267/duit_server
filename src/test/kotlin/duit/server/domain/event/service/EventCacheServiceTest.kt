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

            // 버전 변경: incrementVersion()으로 로컬 버전 캐시 갱신
            every { valueOps.increment("duit:events:v2:version") } returns 6L
            eventCacheService.incrementVersion()

            // 두 번째: version 6 (incrementVersion에서 localVersion이 6으로 갱신됨)
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

    @Nested
    @DisplayName("L1 로컬 메모리 캐시")
    inner class L1LocalCacheTests {

        private val VERSION_KEY = "duit:events:v2:version"

        private fun createJson(vararg ids: Long): String =
            duit.server.application.config.CacheConfig.createCacheObjectMapper()
                .writeValueAsString(createCacheResponse(*ids))

        private fun mockRedisForCache(versionValue: String, jsonValue: String) {
            every { valueOps.get(any<String>()) } answers {
                if (firstArg<String>() == VERSION_KEY) versionValue else jsonValue
            }
        }

        @Test
        @DisplayName("getFromCache 연속 호출 시 L1 캐시 히트 - Redis 조회 최소화")
        fun `L1 캐시 히트하면 Redis를 다시 조회하지 않는다`() {
            val param = EventCursorPaginationParam(size = 10)
            mockRedisForCache("0", createJson(1L, 2L))

            // 1회차: Redis에서 version GET + data GET → L1에 저장
            val result1 = eventCacheService.getFromCache(param)
            // 2회차: 버전 로컬 캐시 히트 + L1 캐시 히트 → Redis 호출 없음
            val result2 = eventCacheService.getFromCache(param)

            assertNotNull(result1)
            assertNotNull(result2)
            assertEquals(2, result1!!.content.size)
            assertEquals(2, result2!!.content.size)
            // Redis GET 총 2회만: 버전 1회 + 데이터 1회 (2회차는 전부 로컬에서 서빙)
            verify(exactly = 2) { valueOps.get(any<String>()) }
        }

        @Test
        @DisplayName("incrementVersion 호출 시 L1 캐시 전체 클리어 - Redis 재조회")
        fun `버전 증가 시 L1 캐시가 클리어되어 Redis를 다시 조회한다`() {
            val param = EventCursorPaginationParam(size = 10)
            mockRedisForCache("0", createJson(1L))

            // 1회차: Redis 조회 → L1에 저장
            eventCacheService.getFromCache(param)

            // 버전 증가 → L1 전체 클리어 + localVersion = 1
            every { valueOps.increment(VERSION_KEY) } returns 1L
            eventCacheService.incrementVersion()

            // 2회차: L1 클리어됨 + 새 버전으로 키 변경 → Redis 재조회
            val result = eventCacheService.getFromCache(param)
            assertNotNull(result)

            // 버전 GET 1회 (첫 조회만, incrementVersion에서 localVersion 직접 갱신)
            // 데이터 GET 2회 (1회차 + 클리어 후 재조회)
            // 총 3회
            verify(exactly = 3) { valueOps.get(any<String>()) }
        }

        @Test
        @DisplayName("서로 다른 파라미터는 L1 캐시에 독립 저장")
        fun `다른 파라미터는 각각 독립적으로 L1 캐시된다`() {
            val param1 = EventCursorPaginationParam(field = PaginationField.CREATED_AT, size = 10)
            val param2 = EventCursorPaginationParam(field = PaginationField.START_DATE, size = 10)
            mockRedisForCache("0", createJson(1L))

            // param1, param2 각각 Redis에서 조회 → L1에 저장
            assertNotNull(eventCacheService.getFromCache(param1))
            assertNotNull(eventCacheService.getFromCache(param2))
            // param1, param2 재조회 → 각각 L1 히트
            assertNotNull(eventCacheService.getFromCache(param1))
            assertNotNull(eventCacheService.getFromCache(param2))

            // 버전 GET 1회 + 데이터 GET 2회 (param1, param2 각 1회) = 총 3회
            verify(exactly = 3) { valueOps.get(any<String>()) }
        }

        @Test
        @DisplayName("L1 캐시 만료 후 Redis 재조회")
        fun `L1 캐시 TTL 만료 시 Redis에서 다시 조회한다`() {
            val param = EventCursorPaginationParam(size = 10)
            mockRedisForCache("0", createJson(1L))

            // 1회차: Redis 조회 → L1에 저장
            assertNotNull(eventCacheService.getFromCache(param))

            // L1 캐시 엔트리의 만료 시간을 과거로 강제 설정
            val localCacheField = EventCacheService::class.java.getDeclaredField("localCache")
            localCacheField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val localCache = localCacheField.get(eventCacheService) as java.util.concurrent.ConcurrentHashMap<String, Any>
            assertFalse(localCache.isEmpty(), "L1 캐시에 엔트리가 있어야 함")

            // 각 엔트리의 expiresAt을 과거 시간(0)으로 교체
            for ((key, _) in localCache) {
                val entryClass = Class.forName("duit.server.domain.event.service.EventCacheService\$LocalCacheEntry")
                val responseField = entryClass.getDeclaredField("response")
                responseField.isAccessible = true
                val currentResponse = responseField.get(localCache[key])
                val expiredEntry = entryClass.constructors[0].newInstance(currentResponse, 0L)
                localCache[key] = expiredEntry
            }

            // 2회차: L1 만료 → Redis 재조회
            assertNotNull(eventCacheService.getFromCache(param))

            // 데이터 Redis GET 2회 (1회차 + 만료 후 재조회)
            verify(exactly = 2) { valueOps.get(match<String> { it != VERSION_KEY }) }
        }

        @Test
        @DisplayName("getFromCache가 L1에 저장한 데이터는 원본과 동일")
        fun `L1 캐시에서 반환된 데이터는 Redis에서 가져온 원본과 동일하다`() {
            val param = EventCursorPaginationParam(size = 10)
            mockRedisForCache("0", createJson(1L, 2L, 3L))

            // Redis에서 조회 (L1 저장)
            val fromRedis = eventCacheService.getFromCache(param)
            // L1에서 조회
            val fromL1 = eventCacheService.getFromCache(param)

            assertNotNull(fromRedis)
            assertNotNull(fromL1)
            assertEquals(fromRedis!!.content.size, fromL1!!.content.size)
            assertEquals(fromRedis.content.map { it.id }, fromL1.content.map { it.id })
            assertEquals(fromRedis.pageInfo.hasNext, fromL1.pageInfo.hasNext)
            assertEquals(fromRedis.pageInfo.nextCursor, fromL1.pageInfo.nextCursor)
        }
    }

    @Nested
    @DisplayName("로컬 버전 캐싱")
    inner class LocalVersionCacheTests {

        private val VERSION_KEY = "duit:events:v2:version"

        @Test
        @DisplayName("연속 호출 시 Redis 버전 GET 1회만 발생")
        fun `버전 조회를 로컬에 캐싱하여 Redis 호출을 줄인다`() {
            val param = EventCursorPaginationParam(size = 10)
            val response = createCacheResponse(1L)

            var versionGetCount = 0
            every { valueOps.get(any<String>()) } answers {
                if (firstArg<String>() == VERSION_KEY) {
                    versionGetCount++
                    "0"
                } else null
            }
            every { valueOps.set(any(), any(), any<Duration>()) } returns Unit

            // putToCache 3회 연속 호출 → 매번 buildCacheKey → getCurrentVersion
            eventCacheService.putToCache(param, response)
            eventCacheService.putToCache(param, response)
            eventCacheService.putToCache(param, response)

            // Redis 버전 GET은 1회만 (로컬 버전 캐싱으로 나머지 2회 절약)
            assertEquals(1, versionGetCount)
        }

        @Test
        @DisplayName("incrementVersion 후 새 버전이 캐시 키에 즉시 반영")
        fun `버전 증가 후 새 버전이 캐시 키에 즉시 반영된다`() {
            val param = EventCursorPaginationParam(size = 10)
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            every { valueOps.get(any<String>()) } returns "5"
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit

            // 버전 5로 저장
            eventCacheService.putToCache(param, response)

            // incrementVersion → localVersion = 10 (Redis 재조회 없이 즉시 반영)
            every { valueOps.increment(VERSION_KEY) } returns 10L
            eventCacheService.incrementVersion()

            // 다시 저장 → 버전 10이 키에 반영되어야 함
            eventCacheService.putToCache(param, response)

            assertEquals(2, capturedKeys.size)
            assertTrue(capturedKeys[0].contains(":5:"), "첫 키에 버전 5")
            assertTrue(capturedKeys[1].contains(":10:"), "두 번째 키에 버전 10")
        }

        @Test
        @DisplayName("Redis 버전 조회 실패 시 마지막 캐시된 버전으로 폴백")
        fun `Redis 장애 시 마지막 캐시된 버전을 사용한다`() {
            val param = EventCursorPaginationParam(size = 10)
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            // 1회차: 정상 → 버전 3 로컬 캐싱
            every { valueOps.get(any<String>()) } returns "3"
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit
            eventCacheService.putToCache(param, response)

            // 로컬 버전 캐시를 만료시켜 Redis 재조회를 유도
            val tsField = EventCacheService::class.java.getDeclaredField("localVersionTimestamp")
            tsField.isAccessible = true
            tsField.setLong(eventCacheService, 0L)

            // 2회차: Redis 장애 → 마지막 캐시된 버전 3으로 폴백
            every { valueOps.get(VERSION_KEY) } throws RuntimeException("Connection refused")
            eventCacheService.putToCache(param, response)

            assertEquals(2, capturedKeys.size)
            assertTrue(capturedKeys[0].contains(":3:"), "정상 시 버전 3")
            assertTrue(capturedKeys[1].contains(":3:"), "장애 시에도 캐시된 버전 3 사용")
        }

        @Test
        @DisplayName("이전 캐시 없이 Redis 장애 시 기본 버전 0 사용")
        fun `캐시된 버전 없이 Redis 장애 시 기본 버전 0을 사용한다`() {
            val param = EventCursorPaginationParam(size = 10)
            val response = createCacheResponse(1L)
            val capturedKeys = mutableListOf<String>()

            // 첫 호출부터 Redis 장애
            every { valueOps.get(any<String>()) } throws RuntimeException("Connection refused")
            every { valueOps.set(capture(capturedKeys), any(), any<Duration>()) } returns Unit
            eventCacheService.putToCache(param, response)

            assertEquals(1, capturedKeys.size)
            assertTrue(capturedKeys[0].contains(":0:"), "캐시 이력 없으면 기본 버전 0")
        }

        @Test
        @DisplayName("incrementVersion 실패 시 로컬 버전/L1 캐시 변경 없음")
        fun `incrementVersion 실패 시 기존 상태가 유지된다`() {
            val param = EventCursorPaginationParam(size = 10)
            val json = duit.server.application.config.CacheConfig.createCacheObjectMapper()
                .writeValueAsString(createCacheResponse(1L))

            // 버전 5로 캐싱 + L1 저장
            every { valueOps.get(any<String>()) } answers {
                if (firstArg<String>() == VERSION_KEY) "5" else json
            }
            assertNotNull(eventCacheService.getFromCache(param))

            // incrementVersion 실패
            every { valueOps.increment(VERSION_KEY) } throws RuntimeException("Redis down")
            eventCacheService.incrementVersion()

            // L1 캐시 여전히 유효 → Redis 재조회 없음
            assertNotNull(eventCacheService.getFromCache(param))

            // 총 Redis GET 2회 (최초 버전 1회 + 데이터 1회)
            // incrementVersion 실패 후에도 L1 캐시가 유지되므로 추가 조회 없음
            verify(exactly = 2) { valueOps.get(any<String>()) }
        }
    }
}
