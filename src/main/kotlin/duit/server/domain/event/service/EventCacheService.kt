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

@Service
class EventCacheService(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val cacheObjectMapper: ObjectMapper = CacheConfig.createCacheObjectMapper()
    private val logger = LoggerFactory.getLogger(javaClass)

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
            val json = redisTemplate.opsForValue().get(key) as? String ?: return null
            cacheObjectMapper.readValue(json, object : TypeReference<CursorPageResponse<EventResponseV2>>() {})
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
            logger.info("Event cache version incremented to {}", newVersion)
        } catch (e: Exception) {
            logger.warn("Version increment failed: {}", e.message)
        }
    }

    private fun getCurrentVersion(): Long {
        return try {
            redisTemplate.opsForValue().get(VERSION_KEY)?.toString()?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            logger.warn("Version read failed: {}", e.message)
            0L
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
