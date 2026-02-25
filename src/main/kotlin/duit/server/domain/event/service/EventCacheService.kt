package duit.server.domain.event.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import duit.server.application.config.CacheConfig
import duit.server.domain.common.dto.pagination.CursorPageResponse
import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.dto.EventCursorPaginationParam
import duit.server.domain.event.dto.EventResponseV2
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service
class EventCacheService(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val cacheObjectMapper: ObjectMapper = CacheConfig.createCacheObjectMapper()
    private val logger = LoggerFactory.getLogger(javaClass)

    // 버전 로컬 캐싱 (Redis 라운드트립 감소)
    @Volatile private var localVersion: Long = -1L
    @Volatile private var localVersionTimestamp: Long = 0L
    private val VERSION_LOCAL_TTL_MS = 2000L

    // L1 로컬 메모리 캐시 (Redis GET + 역직렬화 제거)
    private data class LocalCacheEntry(
        val response: CursorPageResponse<EventResponseV2>,
        val expiresAt: Long
    )
    private val localCache = ConcurrentHashMap<String, LocalCacheEntry>()
    private val LOCAL_CACHE_TTL_MS = 2000L
    companion object {
        private const val VERSION_KEY = "duit:events:v2:version"
        private const val CACHE_KEY_PREFIX = "duit:events:v2"

        private val TTL_MAP = mapOf(
            PaginationField.CREATED_AT to Duration.ofMinutes(5),
            PaginationField.ID to Duration.ofMinutes(5),
            PaginationField.START_DATE to Duration.ofMinutes(3),
            PaginationField.RECRUITMENT_DEADLINE to Duration.ofMinutes(3)
        )
    }

    /**
     * 캐싱 가능 여부 판단
     * bookmarked=true, searchKeyword, hostId가 있으면 캐싱하지 않음
     */
    fun isCacheable(param: EventCursorPaginationParam): Boolean =
        !param.bookmarked
            && param.searchKeyword == null
            && param.hostId == null
            && param.field != PaginationField.VIEW_COUNT

    /**
     * 캐시에서 조회. 캐시 미스 시 null 반환.
     */
    fun getFromCache(param: EventCursorPaginationParam): CursorPageResponse<EventResponseV2>? {
        return try {
            val key = buildCacheKey(param)
            val now = System.currentTimeMillis()

            // L1: 로컬 메모리 캐시 (역직렬화 없음)
            localCache[key]?.let { entry ->
                if (now < entry.expiresAt) return entry.response
                else localCache.remove(key, entry)
            }

            // L2: Redis 캐시
            val json = redisTemplate.opsForValue().get(key) as? String ?: return null
            val response = cacheObjectMapper.readValue(json, object : TypeReference<CursorPageResponse<EventResponseV2>>() {})
            localCache[key] = LocalCacheEntry(response, now + LOCAL_CACHE_TTL_MS)
            response
        } catch (e: Exception) {
            logger.warn("Cache read failed for param={}: {}", param.field, e.message)
            null
        }
    }

    /**
     * DB 조회 결과를 캐시에 저장
     */
    fun putToCache(param: EventCursorPaginationParam, response: CursorPageResponse<EventResponseV2>) {
        try {
            val key = buildCacheKey(param)
            val ttl = TTL_MAP[param.field] ?: Duration.ofMinutes(5)
            val json = cacheObjectMapper.writeValueAsString(response)
            redisTemplate.opsForValue().set(key, json, ttl)
            // Redis 성공 후 L1에도 저장 → 캐시 미스 직후 첫 요청이 Redis 재조회 불필요
            localCache[key] = LocalCacheEntry(response, System.currentTimeMillis() + LOCAL_CACHE_TTL_MS)
        } catch (e: Exception) {
            logger.warn("Cache write failed for param={}: {}", param.field, e.message)
        }
    }

    /**
     * 버전 카운터 증가 (캐시 무효화)
     * 이전 버전의 키는 TTL로 자연 만료됨
     */
    fun incrementVersion() {
        try {
            val newVersion = redisTemplate.opsForValue().increment(VERSION_KEY) ?: 1L
            localVersion = newVersion
            localVersionTimestamp = System.currentTimeMillis()
            localCache.clear()
            logger.info("Event cache version incremented to {}", newVersion)
        } catch (e: Exception) {
            logger.warn("Version increment failed: {}", e.message)
        }
    }

    private fun getCurrentVersion(): Long {
        val now = System.currentTimeMillis()
        if (localVersion >= 0 && now - localVersionTimestamp < VERSION_LOCAL_TTL_MS) {
            return localVersion
        }
        return try {
            val version = redisTemplate.opsForValue().get(VERSION_KEY)?.toString()?.toLongOrNull() ?: 0L
            localVersion = version
            localVersionTimestamp = now
            version
        } catch (e: Exception) {
            logger.warn("Version read failed: {}", e.message)
            if (localVersion >= 0) localVersion else 0L
        }
    }

    /**
     * 캐시 키 생성
     * 패턴: duit:events:v2:{version}:{field}:{statusFilter}:{types}:{size}:{cursor}
     */
    private fun buildCacheKey(param: EventCursorPaginationParam): String {
        val version = getCurrentVersion()
        val field = param.field.name.lowercase()
        val statusFilter = buildStatusFilter(param)
        val types = param.types?.sortedBy { it.name }?.joinToString(",") { it.name.lowercase() } ?: "all"
        val size = param.size
        val cursor = param.cursor ?: "first"

        return "$CACHE_KEY_PREFIX:$version:$field:$statusFilter:$types:$size:$cursor"
    }

    private fun buildStatusFilter(param: EventCursorPaginationParam): String {
        return when {
            param.status != null -> "s:${param.status!!.name.lowercase()}"
            param.statusGroup != null -> "sg:${param.statusGroup!!.name.lowercase()}"
            else -> "sg:active"
        }
    }
}
